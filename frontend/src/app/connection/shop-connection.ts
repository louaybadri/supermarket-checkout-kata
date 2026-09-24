import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { MonoTypeOperatorFunction, retry, tap, throwError, timer } from 'rxjs';

/** How long to wait before sending a request again while the shop cannot be reached. */
const RETRY_AFTER_MS = 3000;

/** How long "Back online" stays on screen once the shop answers again. */
const BACK_ONLINE_FOR_MS = 2000;

/**
 * Whether the backend can be reached, as far as the last request could tell.
 *
 * Nothing is sent in the background to find out: the app only needs the shop when it loads the
 * shelf or prices a cart, so that is when it notices. A request that gets no answer marks the
 * shop unreachable and is sent again every few seconds until one gets through, which marks it
 * reachable again. A small circuit breaker, in the browser.
 */
@Injectable({ providedIn: 'root' })
export class ShopConnection {
  private readonly reachableState = signal(true);

  private readonly backOnlineState = signal(false);

  private backOnlineTimer?: ReturnType<typeof setTimeout>;

  /** False from the first request that gets no answer until one gets through again. */
  readonly reachable = this.reachableState.asReadonly();

  /** True for a moment after the shop answers again, so the shopper sees it came back. */
  readonly backOnline = this.backOnlineState.asReadonly();

  /**
   * An operator for any request to the shop. While the shop cannot be reached the request is
   * sent again every three seconds, for as long as it takes; any other failure is passed on
   * untouched for the caller to handle.
   *
   * Retrying re-subscribes to the request, and re-subscribing to an HTTP observable is what
   * sends it again.
   */
  keepTrying<T>(): MonoTypeOperatorFunction<T> {
    return (request) =>
      request.pipe(
        retry({
          delay: (error) => {
            if (!isUnreachable(error)) {
              return throwError(() => error);
            }
            this.markUnreachable();
            return timer(RETRY_AFTER_MS);
          },
        }),
        tap(() => this.markReachable()),
      );
  }

  private markUnreachable(): void {
    clearTimeout(this.backOnlineTimer);
    this.backOnlineState.set(false);
    this.reachableState.set(false);
  }

  private markReachable(): void {
    // Only a comeback is worth announcing; an answer while all was well changes nothing.
    if (this.reachableState()) {
      return;
    }
    this.reachableState.set(true);
    this.backOnlineState.set(true);
    this.backOnlineTimer = setTimeout(() => this.backOnlineState.set(false), BACK_ONLINE_FOR_MS);
  }
}

/**
 * Whether a failure means the shop gave no answer at all: the browser got nothing back (0), or a
 * proxy or gateway in front of it could not reach it (502, 503, 504). A 500 is different: the
 * shop answered, and said something broke on its side. Retrying that would only hide the bug.
 */
function isUnreachable(error: unknown): boolean {
  return error instanceof HttpErrorResponse && [0, 502, 503, 504].includes(error.status);
}
