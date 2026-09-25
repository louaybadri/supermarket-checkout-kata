import { Product } from './catalog';

/**
 * A picture for each product the shop sells, keyed by sku. Emoji need no image files and no
 * licence. This is presentation only, so it lives here rather than in the backend's catalog; a
 * product added to catalog.yml without an entry here simply shows its first letter.
 */
const ICONS: Record<string, string> = {
  APPLE: '🍎',
  BANANA: '🍌',
  BREAD: '🍞',
  MILK: '🥛',
  CHEESE: '🧀',
  ORANGE: '🍊',
  PEAR: '🍐',
  EGG: '🥚',
  BUTTER: '🧈',
  COFFEE: '☕',
  CROISSANT: '🥐',
  YOGHURT: '🥣',
};

/** The product's icon, or its first letter when it has none. */
export function iconFor(product: Product): string {
  return ICONS[product.sku] ?? product.name.charAt(0);
}
