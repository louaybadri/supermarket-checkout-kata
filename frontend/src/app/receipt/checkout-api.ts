import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** One product on the bill, at shelf price. */
export interface ReceiptLine {
  sku: string;
  name: string;
  quantity: number;
  unitPriceCents: number;
  lineTotalCents: number;
}

/** An offer that fired, how often, and what it took off. */
export interface Discount {
  name: string;
  times: number;
  savingCents: number;
}

export interface Receipt {
  lines: ReceiptLine[];
  discounts: Discount[];
  shelfTotalCents: number;
  totalSavingsCents: number;
  totalCents: number;
}

export interface CartItemRequest {
  sku: string;
  quantity: number;
}

@Injectable({ providedIn: 'root' })
export class CheckoutApi {
  private readonly http = inject(HttpClient);

  receiptFor(items: CartItemRequest[]): Observable<Receipt> {
    return this.http.post<Receipt>('/api/checkout', { items });
  }
}
