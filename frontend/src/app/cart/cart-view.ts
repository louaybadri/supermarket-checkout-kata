import { CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';

import { Cart } from './cart';

/** The cart as the shopper sees it: what is in it, and buttons to change their mind. */
@Component({
  selector: 'app-cart',
  imports: [CurrencyPipe],
  templateUrl: './cart-view.html',
  styleUrl: './cart-view.css',
})
export class CartView {
  protected readonly cart = inject(Cart);
}
