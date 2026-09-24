import { CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';

import { Cart, CartLine } from './cart';

/** The cart as the shopper sees it: what is in it, and buttons to change their mind. */
@Component({
  selector: 'app-cart',
  imports: [CurrencyPipe],
  templateUrl: './cart-view.html',
  styleUrl: './cart-view.css',
})
export class CartView {
  protected readonly cart = inject(Cart);

  /**
   * The shopper typed a number and pressed Enter or left the field. The cart decides what it
   * keeps: the number, the limit, or nothing if it was not a number at all.
   */
  protected setQuantity(line: CartLine, event: Event): void {
    const field = event.target as HTMLInputElement;
    this.cart.setQuantity(line.product.sku, field.valueAsNumber);

    // Written back by hand because the binding alone is not enough: typing 150 on a line already
    // at the limit leaves the cart unchanged, so Angular sees no new value to put in the field
    // and the 150 would stay on screen.
    field.value = String(this.cart.quantityOf(line.product.sku));
  }
}
