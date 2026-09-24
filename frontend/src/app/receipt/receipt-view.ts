import { Component, inject, linkedSignal, signal } from '@angular/core';

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

  protected readonly ringing = signal(false);

  protected checkout(): void {
    this.ringing.set(true);
    this.api.ring(this.cart.toRequestItems()).subscribe({
      next: (receipt) => {
        this.receipt.set(receipt);
        this.ringing.set(false);
      },
      error: (failure) => {
        this.error.set(failure.error?.detail ?? 'The checkout could not price this cart.');
        this.ringing.set(false);
      },
    });
  }

  protected euros(cents: number): string {
    return (cents / 100).toFixed(2) + ' €';
  }
}
