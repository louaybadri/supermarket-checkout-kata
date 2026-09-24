import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Cart } from './cart';
import { CartView } from './cart-view';
import { Product } from './catalog';

const APPLE: Product = { sku: 'APPLE', name: 'Apple', unitPriceCents: 30 };

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

  it('says the cart is empty before anything is picked up', () => {
    expect(page().textContent).toContain('Nothing in it yet.');
  });

  it('shows a line and the running count once something is added', () => {
    cart.add(APPLE);
    cart.add(APPLE);
    fixture.detectChanges();

    expect(page().querySelector('.name')?.textContent).toContain('Apple');
    expect(page().querySelector('.quantity')?.textContent?.trim()).toBe('2');
    expect(page().querySelector('h2')?.textContent).toContain('(2)');
  });

  it('puts one back when the minus button is pressed', () => {
    cart.add(APPLE);
    cart.add(APPLE);
    fixture.detectChanges();

    page().querySelector<HTMLButtonElement>('[aria-label="One less Apple"]')!.click();
    fixture.detectChanges();

    expect(page().querySelector('.quantity')?.textContent?.trim()).toBe('1');
  });

  it('empties the cart when asked', () => {
    cart.add(APPLE);
    fixture.detectChanges();

    page().querySelector<HTMLButtonElement>('.clear')!.click();
    fixture.detectChanges();

    expect(page().textContent).toContain('Nothing in it yet.');
  });
});
