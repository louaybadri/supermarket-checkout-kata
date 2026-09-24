import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Cart } from './cart';
import { CartView } from './cart-view';
import { Product } from '../catalog/catalog';

const APPLE: Product = { sku: 'APPLE', name: 'Apple', unitPriceCents: 30, maxQuantity: 2 };

describe('CartView', () => {
  let fixture: ComponentFixture<CartView>;
  let cart: Cart;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [CartView] }).compileComponents();

    cart = TestBed.inject(Cart);
    cart.clear();
    fixture = TestBed.createComponent(CartView);
    fixture.detectChanges();
  });

  function page(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function quantityField(): HTMLInputElement {
    return page().querySelector<HTMLInputElement>('input.quantity')!;
  }

  /** What a shopper does: type a number, then press Enter or leave the field. */
  function type(value: string): void {
    quantityField().value = value;
    quantityField().dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  it('says the cart is empty before anything is picked up', () => {
    expect(page().textContent).toContain('Nothing in it yet.');
  });

  it('shows a line and the running count once something is added', () => {
    cart.add(APPLE);
    cart.add(APPLE);
    fixture.detectChanges();

    expect(page().querySelector('.name')?.textContent).toContain('Apple');
    expect(quantityField().value).toBe('2');
    expect(page().querySelector('.count')?.textContent?.trim()).toBe('2');
  });

  it('puts one back when the minus button is pressed', () => {
    cart.add(APPLE);
    cart.add(APPLE);
    fixture.detectChanges();

    page().querySelector<HTMLButtonElement>('[aria-label="One less Apple"]')!.click();
    fixture.detectChanges();

    expect(quantityField().value).toBe('1');
  });

  it('takes a quantity typed into the field', () => {
    cart.add(APPLE);
    fixture.detectChanges();

    type('2');

    expect(cart.quantityOf('APPLE')).toBe(2);
    expect(quantityField().value).toBe('2');
  });

  it('shows the limit when more than the shop allows is typed', () => {
    cart.add(APPLE);
    fixture.detectChanges();

    type('150');

    expect(quantityField().value).toBe('2');
  });

  it('puts the quantity back when the field is cleared', () => {
    cart.add(APPLE);
    fixture.detectChanges();

    type('');

    expect(quantityField().value).toBe('1');
  });

  it('turns the plus button off at the most the shop allows', () => {
    cart.add(APPLE);
    fixture.detectChanges();
    const plus = page().querySelector<HTMLButtonElement>('[aria-label="One more Apple"]')!;
    expect(plus.disabled).toBe(false);

    cart.add(APPLE);
    fixture.detectChanges();

    expect(plus.disabled).toBe(true);
  });

  it('empties the cart when asked', () => {
    cart.add(APPLE);
    fixture.detectChanges();

    page().querySelector<HTMLButtonElement>('.clear')!.click();
    fixture.detectChanges();

    expect(page().textContent).toContain('Nothing in it yet.');
  });
});
