import { Component, inject } from '@angular/core';

import { ShopConnection } from './shop-connection';

/** A strip under the top bar that says when the shop cannot be reached, and when it is back. */
@Component({
  selector: 'app-connection-banner',
  templateUrl: './connection-banner.html',
  styleUrl: './connection-banner.css',
})
export class ConnectionBanner {
  protected readonly connection = inject(ShopConnection);
}
