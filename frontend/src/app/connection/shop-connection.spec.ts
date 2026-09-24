import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { defer, Observable, of, throwError } from 'rxjs';

import { ShopConnection } from './shop-connection';

/** A request that fails with the given statuses, one per attempt, then answers 'shelf'. */
function failingWith(...statuses: number[]): {
  request: Observable<string>;
  attempts: () => number;
} {
  let attempts = 0;
  const request = defer(() => {
    const status = statuses[attempts++];
    return status === undefined ? of('shelf') : throwError(() => new HttpErrorResponse({ status }));
  });
  return { request, attempts: () => attempts };
}

describe('ShopConnection', () => {
  let connection: ShopConnection;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({});
    connection = TestBed.inject(ShopConnection);
  });

  afterEach(() => vi.useRealTimers());

  it('starts out assuming the shop is there', () => {
    expect(connection.reachable()).toBe(true);
    expect(connection.backOnline()).toBe(false);
  });

  it('says the shop is unreachable and tries again three seconds later', () => {
    const { request, attempts } = failingWith(502);
    const answers: string[] = [];

    request.pipe(connection.keepTrying()).subscribe((answer) => answers.push(answer));

    expect(connection.reachable()).toBe(false);
    expect(attempts()).toBe(1);

    vi.advanceTimersByTime(3000);

    expect(attempts()).toBe(2);
    expect(answers).toEqual(['shelf']);
  });

  it('says so for a short while when the shop is back', () => {
    const { request } = failingWith(0);
    request.pipe(connection.keepTrying()).subscribe();

    vi.advanceTimersByTime(3000);

    expect(connection.reachable()).toBe(true);
    expect(connection.backOnline()).toBe(true);

    vi.advanceTimersByTime(2000);

    expect(connection.backOnline()).toBe(false);
  });

  it('keeps trying for as long as the shop stays away', () => {
    const { request, attempts } = failingWith(503, 504, 0);
    request.pipe(connection.keepTrying()).subscribe();

    vi.advanceTimersByTime(9000);

    expect(attempts()).toBe(4);
    expect(connection.reachable()).toBe(true);
  });

  it('passes a refusal of the cart straight through, without trying again', () => {
    const { request, attempts } = failingWith(400);
    let failure: HttpErrorResponse | undefined;

    request.pipe(connection.keepTrying()).subscribe({ error: (error) => (failure = error) });
    vi.advanceTimersByTime(3000);

    expect(failure?.status).toBe(400);
    expect(attempts()).toBe(1);
    expect(connection.reachable()).toBe(true);
  });

  it('does not retry a fault on the shop side, which would only hide it', () => {
    const { request, attempts } = failingWith(500);
    let failure: HttpErrorResponse | undefined;

    request.pipe(connection.keepTrying()).subscribe({ error: (error) => (failure = error) });
    vi.advanceTimersByTime(3000);

    expect(failure?.status).toBe(500);
    expect(attempts()).toBe(1);
    expect(connection.reachable()).toBe(true);
  });
});
