import { Component } from '@angular/core';

import { CartView } from './cart-view';
import { ProductList } from './product-list';

@Component({
  imports: [ProductList, CartView],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {}
