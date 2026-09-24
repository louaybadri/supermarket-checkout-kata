import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Cart } from '../cart/cart';
import { Product } from '../catalog/catalog';
import { ReceiptView } from './receipt-view';

const APPLE: Product = { sku: 'APPLE', name: 'Apple', unitPriceCents: 30, maxQuantity: 99 };

const THREE_APPLES_RECEIPT = {
  lines: [{ sku: 'APPLE', name: 'Apple', quantity: 3, unitPriceCents: 30, lineTotalCents: 90 }],
  discounts: [{ name: '2 apples for 0.45', times: 1, savingCents: 15 }],
  shelfTotalCents: 90,
  totalSavingsCents: 15,
  totalCents: 75,
};

describe('ReceiptView', () => {
  let fixture: ComponentFixture<ReceiptView>;
  let http: HttpTestingController;
  let cart: Cart;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReceiptView],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    cart = TestBed.inject(Cart);
    cart.clear();
    fixture = TestBed.createComponent(ReceiptView);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function page(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function pressCheckout(): void {
    page().querySelector<HTMLButtonElement>('.checkout')!.click();
    fixture.detectChanges();
  }

  it('cannot check out an empty cart', () => {
    expect(page().querySelector<HTMLButtonElement>('.checkout')!.disabled).toBe(true);
  });

  it('sends the cart and prints what the backend charges', () => {
    cart.add(APPLE);
    cart.add(APPLE);
    cart.add(APPLE);
    fixture.detectChanges();

    pressCheckout();

    const call = http.expectOne('/api/checkout');
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual({ items: [{ sku: 'APPLE', quantity: 3 }] });

    call.flush(THREE_APPLES_RECEIPT);
    fixture.detectChanges();

    expect(page().querySelector('.line')?.textContent).toContain('Apple');
    expect(page().querySelector('.discount')?.textContent).toContain('2 apples for 0.45');
    // The same format as the prices on the shelf and in the cart.
    expect(page().querySelector('.line .amount')?.textContent).toContain('€0.90');
    expect(page().querySelector('.discount')?.textContent).toContain('€0.15');
    expect(page().querySelector('.total')?.textContent).toContain('€0.75');
    expect(page().querySelector('.saved')?.textContent).toContain('€0.15');
  });

  it('shows what the backend says when it refuses the cart', () => {
    cart.add(APPLE);
    fixture.detectChanges();

    pressCheckout();

    http
      .expectOne('/api/checkout')
      .flush({ title: 'Unknown product', detail: "The shop does not sell 'UNICORN'" },
        { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(page().querySelector('.error')?.textContent).toContain("does not sell 'UNICORN'");
  });

  it('takes the bill down as soon as the cart changes', () => {
    cart.add(APPLE);
    fixture.detectChanges();

    pressCheckout();
    http.expectOne('/api/checkout').flush(THREE_APPLES_RECEIPT);
    fixture.detectChanges();
    expect(page().querySelector('.receipt')).not.toBeNull();

    cart.add(APPLE);
    fixture.detectChanges();

    expect(page().querySelector('.receipt')).toBeNull();
  });

  it('drops the reply for a cart that changed while it was being priced', () => {
    cart.add(APPLE);
    fixture.detectChanges();
    pressCheckout();
    const call = http.expectOne('/api/checkout');

    // Another apple goes in before the backend has answered for one.
    cart.add(APPLE);
    fixture.detectChanges();

    expect(call.cancelled).toBe(true);
    expect(page().querySelector('.receipt')).toBeNull();
    expect(page().querySelector('.checkout')?.textContent).toContain('Checkout');
  });

  // The two tests below read the component's state straight after the cart changes, with no
  // change detection in between, so they fail if the reset waits for Angular's next render.

  it('forgets the bill the moment the cart changes', () => {
    cart.add(APPLE);
    fixture.detectChanges();
    pressCheckout();
    http.expectOne('/api/checkout').flush(THREE_APPLES_RECEIPT);

    cart.add(APPLE);

    expect(fixture.componentInstance['receipt']()).toBeNull();
  });

  it('forgets the error the moment the cart changes', () => {
    cart.add(APPLE);
    fixture.detectChanges();
    pressCheckout();
    http
      .expectOne('/api/checkout')
      .flush(
        { detail: "The shop does not sell 'UNICORN'" },
        { status: 400, statusText: 'Bad Request' },
      );

    cart.add(APPLE);

    expect(fixture.componentInstance['error']()).toBeNull();
  });
});
