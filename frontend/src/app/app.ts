import { Component } from '@angular/core';

import { CartView } from './cart/cart-view';
import { ProductList } from './catalog/product-list';

@Component({
  imports: [ProductList, CartView],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {}
