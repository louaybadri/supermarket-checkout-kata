import { Component } from '@angular/core';

import { CartView } from './cart/cart-view';
import { ProductList } from './catalog/product-list';
import { ReceiptView } from './receipt/receipt-view';

@Component({
  imports: [ProductList, CartView, ReceiptView],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {}
