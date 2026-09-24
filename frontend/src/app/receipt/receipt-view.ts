import { Component, effect, inject, signal } from '@angular/core';

import { Cart } from '../cart/cart';
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

  protected readonly receipt = signal<Receipt | null>(null);

  protected readonly error = signal<string | null>(null);

  protected readonly ringing = signal(false);

  constructor() {
    // A bill printed a moment ago is wrong as soon as the cart changes, so it goes away.
    effect(() => {
      this.cart.lines();
      this.receipt.set(null);
      this.error.set(null);
    });
  }

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
