import { Component, inject, linkedSignal, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { catchError, EMPTY, finalize, skip, Subject, switchMap, takeUntil } from 'rxjs';

import { Cart, CartLine } from '../cart/cart';
import { CheckoutApi, Receipt } from './checkout-api';

/**
 * The till. Sends the cart to the backend and prints what comes back; it never works out a
 * price itself.
 */
@Component({
  selector: 'app-receipt',
  imports: [],
  templateUrl: './receipt-view.html',
  styleUrl: './receipt-view.css',
})
export class ReceiptView {
  private readonly api = inject(CheckoutApi);

  protected readonly cart = inject(Cart);

  /**
   * The bill for the cart as it was when checkout was pressed. It is linked to the cart: the
   * moment the cart changes it goes back to null, because a bill printed a moment ago is wrong as
   * soon as something is added or put back. Until then it can be set like any signal.
   */
  protected readonly receipt = linkedSignal<CartLine[], Receipt | null>({
    source: this.cart.lines,
    computation: () => null,
  });

  /** What the backend said when it refused the cart, cleared the same way as the bill. */
  protected readonly error = linkedSignal<CartLine[], string | null>({
    source: this.cart.lines,
    computation: () => null,
  });

  /** True while the backend is pricing the cart. */
  protected readonly pricing = signal(false);

  /** One event per press of the checkout button. */
  private readonly checkoutPresses = new Subject<void>();

  /**
   * Every change to the cart after this moment. `toObservable` starts by replaying the cart
   * as it is now, which is not a change, so the first value is skipped.
   */
  private readonly cartChanges = toObservable(this.cart.lines).pipe(skip(1));

  constructor() {
    this.checkoutPresses
      .pipe(
        // switchMap: a new press drops the request still waiting for its answer, so only the
        // latest one can print a bill. mergeMap would let both answers race, concatMap would
        // queue them, exhaustMap would ignore the new press.
        switchMap(() => {
          // Set here rather than in a tap before switchMap: switchMap cancels the previous
          // request first, and its finalize would otherwise switch this back off.
          this.pricing.set(true);
          return this.api.receiptFor(this.cart.toRequestItems()).pipe(
            // The cart changed while the backend was pricing it, so the answer is for a cart
            // that no longer exists: unsubscribing cancels the HTTP request and nothing prints.
            takeUntil(this.cartChanges),
            // Caught inside, so a refused cart does not end the stream for the next press.
            catchError((failure) => {
              this.error.set(failure.error?.detail ?? 'The checkout could not price this cart.');
              return EMPTY;
            }),
            // Runs when the answer arrives, when it fails, and when it is dropped.
            finalize(() => this.pricing.set(false)),
          );
        }),
        // Stops listening when the component goes away, so nothing leaks.
        takeUntilDestroyed(),
      )
      .subscribe((receipt) => this.receipt.set(receipt));
  }

  protected checkout(): void {
    this.checkoutPresses.next();
  }

  protected euros(cents: number): string {
    return (cents / 100).toFixed(2) + ' €';
  }
}
