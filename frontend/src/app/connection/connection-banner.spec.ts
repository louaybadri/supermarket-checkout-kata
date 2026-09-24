import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { defer, of, throwError } from 'rxjs';

import { ConnectionBanner } from './connection-banner';
import { ShopConnection } from './shop-connection';

describe('ConnectionBanner', () => {
  let fixture: ComponentFixture<ConnectionBanner>;
  let connection: ShopConnection;

  beforeEach(async () => {
    vi.useFakeTimers();
    await TestBed.configureTestingModule({ imports: [ConnectionBanner] }).compileComponents();
    connection = TestBed.inject(ShopConnection);
    fixture = TestBed.createComponent(ConnectionBanner);
    fixture.detectChanges();
  });

  afterEach(() => vi.useRealTimers());

  function page(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  /** A request that gets no answer once, then gets through. */
  function shopDownOnce(): void {
    let attempts = 0;
    defer(() =>
      attempts++ === 0 ? throwError(() => new HttpErrorResponse({ status: 502 })) : of('ok'),
    )
      .pipe(connection.keepTrying())
      .subscribe();
    fixture.detectChanges();
  }

  it('shows nothing while the shop answers', () => {
    expect(page().textContent?.trim()).toBe('');
  });

  it('says the shop cannot be reached, and that it keeps trying', () => {
    shopDownOnce();

    expect(page().querySelector('[role="alert"]')?.textContent).toContain(
      "Can't reach the shop. Trying again…",
    );
  });

  it('says the shop is back, then goes away', () => {
    shopDownOnce();

    vi.advanceTimersByTime(3000);
    fixture.detectChanges();
    expect(page().querySelector('[role="status"]')?.textContent).toContain('Back online');

    vi.advanceTimersByTime(2000);
    fixture.detectChanges();
    expect(page().textContent?.trim()).toBe('');
  });
});
