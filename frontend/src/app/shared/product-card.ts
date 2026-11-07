import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartService } from '../core/cart.service';
import { ProductSummary } from '../core/models';
import { DtPipe, discount } from '../core/pipes';
import { CompareService, WishlistService } from '../core/stores';
import { Icon } from './icon';
import { Stars } from './stars';

/** Seuil d'unités vendues pour le badge « Meilleure vente ». */
export const BEST_SELLER_MIN = 10;

@Component({
  selector: 'app-product-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, DtPipe, Icon, Stars],
  template: `
    @let p = product();
    <article class="pc">
      <div class="pc-media photo-stage">
        <a [routerLink]="['/produit', p.slug]" [attr.aria-label]="p.name" class="pc-img">
          @if (p.image) {
            <img [src]="p.image" [alt]="p.name" loading="lazy" />
          } @else {
            <span class="img-placeholder"><app-icon name="image" [size]="36" [stroke]="1.5" /></span>
          }
        </a>
        <div class="pc-flags">
          @if (off(); as o) { <span class="badge badge-promo">-{{ o }} %</span> }
          @if (p.soldCount >= bestSellerMin) { <span class="badge badge-best">Meilleure vente</span> }
        </div>
        <button type="button" class="pc-wish" [class.on]="wished()" (click)="wishlist.toggle(p)"
                [attr.aria-pressed]="wished()" [attr.aria-label]="(wished() ? 'Retirer des favoris : ' : 'Ajouter aux favoris : ') + p.name">
          <app-icon [name]="wished() ? 'heartFill' : 'heart'" [size]="18" />
        </button>
      </div>
      <div class="pc-body">
        @if (p.brand) { <span class="pc-brand">{{ p.brand }}</span> }
        <a class="pc-name" [routerLink]="['/produit', p.slug]">{{ p.name }}</a>
        @if (p.ratingCount > 0) {
          <app-stars [value]="p.ratingAvg" [count]="p.ratingCount" [size]="14" />
        }
        <div class="pc-price">
          <span class="price">{{ p.price | dt }}</span>
          @if (p.oldPrice && off()) { <span class="old-price">{{ p.oldPrice | dt }}</span> }
        </div>
        @if (p.stock === 0) {
          <span class="stock out-stock">Rupture de stock</span>
        } @else if (p.stock <= 5) {
          <span class="stock low-stock">Plus que {{ p.stock }} en stock</span>
        } @else {
          <span class="stock in-stock">En stock</span>
        }
        <div class="pc-actions">
          <button type="button" class="btn btn-dark btn-sm grow add" [class.done]="added()" [disabled]="p.stock === 0" (click)="add()"
                  [attr.aria-label]="(added() ? 'Ajouté au panier : ' : 'Ajouter au panier : ') + p.name">
            <app-icon [name]="added() ? 'check' : 'cart'" [size]="16" />
            <span class="add-label">{{ added() ? 'Ajouté' : 'Ajouter' }}</span>
          </button>
          <button type="button" class="btn btn-outline btn-sm cmp" [class.on]="compared()" (click)="toggleCompare()"
                  [attr.aria-pressed]="compared()" [attr.aria-label]="'Comparer ' + p.name" [title]="compareHint()">
            <app-icon name="compare" [size]="16" />
          </button>
        </div>
      </div>
    </article>
  `,
  styles: `
    :host { display: block; container-type: inline-size; }
    .pc { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius); overflow: hidden;
      display: flex; flex-direction: column; height: 100%; transition: box-shadow .2s, transform .2s, border-color .2s; }
    .pc:hover { box-shadow: var(--shadow-lift); border-color: transparent; }
    .pc-media { position: relative; aspect-ratio: 1; margin: 6px 6px 0; border-radius: calc(var(--radius) - 6px); overflow: hidden; }
    .pc-img { display: block; width: 100%; height: 100%; }
    .pc-img img { width: 100%; height: 100%; object-fit: contain; padding: 7%; transition: transform .35s ease; }
    .pc:hover .pc-img img { transform: scale(1.05); }
    .pc-flags { position: absolute; top: 10px; left: 10px; display: flex; flex-direction: column; gap: 6px; align-items: flex-start; }
    .pc-wish { position: absolute; top: 8px; right: 8px; width: 38px; height: 38px; border-radius: 50%; border: 0;
      background: rgba(255,255,255,.9); color: #2b3658; display: flex; align-items: center; justify-content: center;
      cursor: pointer; box-shadow: 0 1px 6px rgba(12,22,51,.14); transition: transform .15s; }
    .pc-wish:active { transform: scale(.9); }
    .pc-wish.on { color: var(--bloom); }
    .pc-body { padding: 12px 14px 14px; display: flex; flex-direction: column; gap: 5px; flex: 1; }
    .pc-brand { font-size: 12px; font-weight: 600; color: var(--blue); }
    .pc-name { font-weight: 500; color: var(--ink); text-decoration: none; line-height: 1.35; font-size: 15px;
      display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
    .pc-name:hover { color: var(--blue); }
    .pc-price { display: flex; gap: 8px; align-items: baseline; margin-top: auto; padding-top: 4px; flex-wrap: wrap; }
    .stock { font-size: 12px; font-weight: 500; display: inline-flex; align-items: center; gap: 6px; }
    .stock::before { content: ''; width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
    .low-stock { color: var(--bloom); font-weight: 600; }
    .pc-actions { display: flex; gap: 8px; margin-top: 8px; }
    .grow { flex: 1; padding: 0 12px; }
    .add.done { background: var(--green); color: #fff; }
    .cmp { padding: 0; width: 38px; flex: none; }
    .cmp.on { border-color: var(--blue); background: var(--blue-soft); color: var(--blue); }

    /* carte étroite (2 colonnes sur mobile) */
    @container (max-width: 200px) {
      .pc-body { padding: 10px 10px 12px; }
      .pc-name { font-size: 14px; }
      .price { font-size: 16px; }
      .old-price { font-size: 12px; }
      .pc-flags .badge { font-size: 11px; padding: 2px 8px; }
      .pc-wish { width: 34px; height: 34px; }
      .cmp { display: none; }
    }
  `,
})
export class ProductCard {
  private cart = inject(CartService);
  protected wishlist = inject(WishlistService);
  private compare = inject(CompareService);
  product = input.required<ProductSummary>();

  protected readonly bestSellerMin = BEST_SELLER_MIN;
  protected added = signal(false);
  protected off = computed(() => discount(this.product().price, this.product().oldPrice));
  protected wished = computed(() => this.wishlist.ids().includes(this.product().id));
  protected compared = computed(() => this.compare.ids().includes(this.product().id));
  protected compareHint = computed(() =>
    this.compared() ? 'Retirer du comparateur' : this.compare.full() ? 'Comparateur plein (4 produits)' : 'Ajouter au comparateur',
  );

  add() {
    this.cart.add(this.product());
    this.added.set(true);
    setTimeout(() => this.added.set(false), 1500);
  }

  toggleCompare() {
    if (!this.compare.toggle(this.product())) {
      alert('Le comparateur contient déjà 4 produits. Retirez-en un pour en ajouter un autre.');
    }
  }
}
