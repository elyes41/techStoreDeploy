import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { ApiService } from '../core/api.service';
import { CartService } from '../core/cart.service';
import { ProductSummary } from '../core/models';
import { DtPipe, discount } from '../core/pipes';
import { RecentlyViewedService } from '../core/stores';
import { ProductCarousel } from '../shared/carousel';
import { Icon } from '../shared/icon';
import { ProductCard } from '../shared/product-card';

const SPOTLIGHT_MS = 6000;

@Component({
  selector: 'app-home',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ProductCard, Icon, ProductCarousel, DtPipe],
  template: `
    <!-- Vitrine : un produit mis en avant dans l'arche (porte de Sidi Bou Saïd) -->
    <section class="container hero">
      <div class="hero-text">
        <h1>La tech qu'il vous faut, livrée partout en Tunisie.</h1>
        <p class="lead">Smartphones, PC, accessoires et électroménager, avec garantie officielle. Vous payez à la livraison si vous préférez.</p>
        <div class="row">
          <a routerLink="/catalogue" class="btn btn-primary">Voir le catalogue</a>
          <a routerLink="/catalogue" [queryParams]="{ promo: true }" class="btn btn-outline">Offres du moment</a>
        </div>
      </div>

      <div class="vitrine" (mouseenter)="paused.set(true)" (mouseleave)="paused.set(false)"
           (focusin)="paused.set(true)" (focusout)="paused.set(false)">
        <div class="arch-frame" aria-hidden="true"></div>
        <div class="arch photo-stage">
          @if (spot(); as s) {
            @if (s.image) {
              @for (img of [s.image]; track img) { <img class="spot-img" [src]="img" [alt]="s.name" /> }
            }
          } @else {
            <span class="arch-empty"><app-icon name="image" [size]="48" [stroke]="1.2" /></span>
          }
        </div>
        @if (spot(); as s) {
          <div class="spot-card" aria-live="polite">
            <a class="spot-name" [routerLink]="['/produit', s.slug]">{{ s.name }}</a>
            <div class="spot-row">
              <span class="price">{{ s.price | dt }}</span>
              @if (spotOff(); as o) { <span class="badge badge-promo">-{{ o }} %</span> }
              <span class="spacer"></span>
              <button type="button" class="btn btn-primary btn-sm spot-add" [disabled]="s.stock === 0" (click)="add(s)"
                      [attr.aria-label]="'Ajouter au panier : ' + s.name">
                <app-icon [name]="added() ? 'check' : 'cart'" [size]="16" />
              </button>
            </div>
          </div>
          @if (spotlight().length > 1) {
            <div class="spot-nav" role="group" aria-label="Produits en vitrine">
              @for (p of spotlight(); track p.id; let i = $index) {
                <button type="button" class="pip" [class.on]="i === index()" (click)="go(i)"
                        [attr.aria-label]="'Afficher ' + p.name" [attr.aria-current]="i === index()"></button>
              }
            </div>
          }
        }
      </div>
    </section>

    <section class="container perks" aria-label="Nos engagements">
      <div class="perk"><app-icon name="truck" [size]="22" /><span><strong>Livraison rapide</strong> partout en Tunisie</span></div>
      <div class="perk"><app-icon name="card" [size]="22" /><span><strong>Paiement à la livraison</strong> ou par carte</span></div>
      <div class="perk"><app-icon name="shield" [size]="22" /><span><strong>Garantie officielle</strong> sur tout le catalogue</span></div>
      <div class="perk"><app-icon name="refresh" [size]="22" /><span><strong>Retour facile</strong> après réception</span></div>
    </section>

    @if (categoryTiles().length) {
      <section class="container block">
        <div class="block-head">
          <h2>Les rayons</h2>
          <a routerLink="/catalogue" class="see-all">Tout le catalogue</a>
        </div>
        <div class="rail">
          @for (c of categoryTiles(); track c.id) {
            <a class="tile" [routerLink]="['/catalogue', c.slug]">
              <span class="tile-arch photo-stage">
                @if (c.image) { <img [src]="c.image" alt="" loading="lazy" /> }
                @else { <span class="tile-initial">{{ c.name.charAt(0) }}</span> }
              </span>
              <strong>{{ c.name }}</strong>
              <span class="muted small">{{ c.productCount }} produit{{ c.productCount > 1 ? 's' : '' }}</span>
            </a>
          }
        </div>
      </section>
    }

    @if (forYou().length) {
      <div class="container block">
        <app-product-carousel title="Recommandé pour vous" subtitle="D'après les produits que vous avez consultés" [products]="forYou()" />
      </div>
    }

    @if (trending().length) {
      <div class="container block">
        <app-product-carousel title="Tendances de la semaine" subtitle="Les plus vus et les plus achetés ces 7 derniers jours" [products]="trending()" />
      </div>
    }

    @if (promos()?.length) {
      <section class="container block">
        <div class="promo-banner">
          <div class="promo-text">
            <h2>Les promos du moment</h2>
            <p>Des remises sur une sélection de produits, tant qu'il reste du stock.</p>
          </div>
          <a routerLink="/catalogue" [queryParams]="{ promo: true }" class="btn promo-btn">Voir toutes les promos</a>
        </div>
        <div class="product-grid">
          @for (p of promos(); track p.id) { <app-product-card [product]="p" /> }
        </div>
      </section>
    }

    <section class="container block">
      <div class="block-head">
        <h2>Produits phares</h2>
        <a routerLink="/catalogue" class="see-all">Tout voir</a>
      </div>
      @if (featured() === null) {
        <div class="spinner" role="status" aria-label="Chargement"></div>
      } @else if (featured()!.length) {
        <div class="product-grid">
          @for (p of featured(); track p.id) { <app-product-card [product]="p" /> }
        </div>
      } @else {
        <p class="empty">Aucun produit mis en avant. Cochez « Mis en avant » sur un produit dans l'administration.</p>
      }
    </section>

    @if (recent.items().length) {
      <div class="container block">
        <app-product-carousel title="Récemment consultés" [products]="recent.items()" />
      </div>
    }
  `,
  styles: `
    /* ---------- Vitrine ---------- */
    .hero { display: grid; grid-template-columns: minmax(0, 1.05fr) minmax(0, .95fr); gap: clamp(24px, 5vw, 64px); align-items: center;
      padding-top: clamp(24px, 5vw, 56px); padding-bottom: 24px; }
    .hero-text { display: flex; flex-direction: column; gap: 22px; align-items: flex-start; }
    .hero h1 { font-size: clamp(30px, 5vw, 58px); line-height: 1.08; letter-spacing: -0.015em; max-width: 14ch; }
    .lead { font-size: clamp(16px, 1.6vw, 19px); color: var(--ink-2); max-width: 46ch; }

    .vitrine { position: relative; justify-self: center; width: min(100%, 400px); padding-bottom: 56px; }
    /* l'encadrement de porte : arche cobalt décalée */
    .arch-frame { position: absolute; inset: 18px -18px 74px 18px; border-radius: var(--arch); background: var(--blue);
      animation: frame-in .9s cubic-bezier(.2,.8,.2,1) both; }
    .arch { position: relative; aspect-ratio: 5 / 6; border-radius: var(--arch); overflow: hidden; border: 8px solid var(--surface);
      box-shadow: var(--shadow-lift); display: flex; align-items: center; justify-content: center;
      animation: arch-in .9s cubic-bezier(.2,.8,.2,1) .1s both; }
    .spot-img { width: 100%; height: 100%; object-fit: contain; padding: 16% 12% 22%; animation: spot-in .6s ease both; }
    .arch-empty { color: #9aa6c8; }
    .spot-card { position: absolute; left: -12px; right: 24px; bottom: 0; background: var(--glass); backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px); border: 1px solid var(--line); border-radius: var(--radius); padding: 14px 14px 14px 18px;
      box-shadow: var(--shadow-lift); display: flex; flex-direction: column; gap: 6px; animation: card-in .7s cubic-bezier(.2,.8,.2,1) .45s both; }
    .spot-name { color: var(--ink); text-decoration: none; font-weight: 600; line-height: 1.3;
      display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
    .spot-name:hover { color: var(--blue); }
    .spot-row { display: flex; align-items: center; gap: 10px; }
    .spot-add { width: 42px; padding: 0; }
    .spot-nav { position: absolute; right: -6px; top: 50%; transform: translateY(-60%); display: flex; flex-direction: column; gap: 8px; }
    .pip { width: 10px; height: 10px; border-radius: 999px; border: 0; padding: 0; background: var(--line-2); cursor: pointer;
      transition: height .25s, background .25s; }
    .pip.on { height: 28px; background: var(--ink); }

    @keyframes frame-in { from { clip-path: inset(100% 0 0 0 round 999px 999px 22px 22px); } to { clip-path: inset(0 0 0 0 round 999px 999px 22px 22px); } }
    @keyframes arch-in { from { opacity: 0; transform: translateY(24px) scale(.97); } }
    @keyframes spot-in { from { opacity: 0; transform: scale(.94); } }
    @keyframes card-in { from { opacity: 0; transform: translateY(16px); } }

    /* ---------- Engagements ---------- */
    .perks { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; padding-top: 16px; padding-bottom: 8px; }
    .perk { display: flex; gap: 12px; align-items: center; padding: 14px 16px; border-radius: var(--radius); background: var(--surface);
      border: 1px solid var(--line); color: var(--blue); font-size: 14px; line-height: 1.35; }
    .perk span { color: var(--ink-2); }
    .perk strong { color: var(--ink); font-weight: 600; display: block; }

    /* ---------- Blocs ---------- */
    .block { padding-top: clamp(28px, 4vw, 44px); display: flex; flex-direction: column; gap: 18px; }
    .block-head { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; }
    .see-all { font-weight: 600; text-decoration: none; white-space: nowrap; }

    /* ---------- Rayons : tuiles en arche ---------- */
    .rail { display: grid; grid-auto-flow: column; grid-auto-columns: 148px; gap: 14px; overflow-x: auto; padding: 2px 2px 10px;
      scroll-snap-type: x mandatory; scrollbar-width: none; overscroll-behavior-x: contain; }
    .rail::-webkit-scrollbar { display: none; }
    .tile { scroll-snap-align: start; display: flex; flex-direction: column; gap: 4px; text-decoration: none; color: var(--ink); }
    .tile strong { font-weight: 600; font-size: 14px; line-height: 1.3; margin-top: 6px; }
    .tile-arch { display: flex; align-items: center; justify-content: center; aspect-ratio: 4 / 5; border-radius: 999px 999px 16px 16px;
      overflow: hidden; border: 1px solid var(--line); transition: border-color .2s, transform .2s; }
    .tile-arch img { width: 100%; height: 100%; object-fit: contain; padding: 18% 14% 14%; }
    .tile:hover .tile-arch { border-color: var(--blue); transform: translateY(-3px); }
    .tile-initial { font: 600 40px var(--font-display); color: var(--blue); }

    /* ---------- Bannière promos ---------- */
    .promo-banner { position: relative; overflow: hidden; background: var(--bloom); color: #fff; border-radius: 28px; padding: clamp(22px, 4vw, 36px);
      display: flex; flex-wrap: wrap; gap: 18px; align-items: center; justify-content: space-between; }
    /* deux arches en filigrane */
    .promo-banner::before, .promo-banner::after { content: ''; position: absolute; border-radius: 999px 999px 0 0; border: 2px solid rgba(255,255,255,.28); }
    .promo-banner::before { width: 180px; height: 240px; right: 8%; bottom: -70px; }
    .promo-banner::after { width: 120px; height: 170px; right: calc(8% + 30px); bottom: -70px; }
    .promo-text { position: relative; display: flex; flex-direction: column; gap: 8px; max-width: 48ch; }
    .promo-text p { color: rgba(255,255,255,.88); }
    .promo-btn { position: relative; background: #fff; color: #7a0e3c; }
    .promo-btn:hover { background: #fff; color: #7a0e3c; filter: brightness(.96); }

    @media (max-width: 900px) {
      .perks { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    }
    @media (max-width: 760px) {
      /* mobile : titre, puis la vitrine, puis le texte et les boutons */
      .hero { grid-template-columns: 1fr; gap: 20px; }
      .hero-text { display: contents; }
      .hero h1 { order: 1; max-width: 16ch; font-size: clamp(27px, 7.6vw, 36px); }
      .vitrine { order: 2; width: min(76%, 320px); margin: 8px 0 4px; }
      .lead { order: 3; }
      .hero-text .row { order: 4; }
      .spot-card { left: -18px; right: -18px; padding: 10px 10px 10px 14px; }
      .spot-name { font-size: 14px; }
      .spot-card .price { font-size: 17px; }
      .rail { grid-auto-columns: 116px; gap: 12px; margin: 0 calc(-1 * clamp(16px, 3vw, 28px)); padding-left: clamp(16px, 3vw, 28px);
        padding-right: clamp(16px, 3vw, 28px); scroll-padding-left: clamp(16px, 3vw, 28px); }
      .perks { gap: 8px; }
      .perk { padding: 12px; font-size: 13px; gap: 10px; }
    }
  `,
})
export class HomePage {
  private api = inject(ApiService);
  private cart = inject(CartService);
  protected recent = inject(RecentlyViewedService);

  protected trending = toSignal(this.api.trending(10).pipe(catchError(() => of([]))), { initialValue: [] });
  // « Pour vous » seulement si le visiteur a déjà consulté des produits
  protected forYou = toSignal(
    this.recent.ids().length
      ? this.api.forYou(this.recent.ids().slice(0, 8), 10).pipe(catchError(() => of([])))
      : of([] as ProductSummary[]),
    { initialValue: [] },
  );
  protected categories = toSignal(this.api.categories().pipe(catchError(() => of([]))), { initialValue: [] });
  protected featured = toSignal<ProductSummary[] | null>(
    this.api.products({ featured: true, size: 8 }).pipe(map((r) => r.items), catchError(() => of([]))),
    { initialValue: null },
  );
  protected promos = toSignal<ProductSummary[] | null>(
    this.api.products({ promo: true, size: 4 }).pipe(map((r) => r.items), catchError(() => of([]))),
    { initialValue: null },
  );
  /** Un échantillon de produits pour illustrer chaque rayon. */
  private sample = toSignal(
    this.api.products({ sort: 'best_sellers', size: 60 }).pipe(map((r) => r.items), catchError(() => of([] as ProductSummary[]))),
    { initialValue: [] as ProductSummary[] },
  );

  protected categoryTiles = computed(() => {
    const byCat = new Map<string, string>();
    for (const p of [...(this.featured() ?? []), ...this.trending(), ...this.sample()]) {
      if (p.categoryId && p.image && !byCat.has(p.categoryId)) byCat.set(p.categoryId, p.image);
    }
    return this.categories()
      .filter((c) => c.productCount > 0)
      .map((c) => ({ ...c, image: byCat.get(c.id) ?? null }));
  });

  // ---------- Vitrine ----------
  protected spotlight = computed(() => {
    const pool = [...(this.featured() ?? []), ...(this.promos() ?? []), ...this.trending()];
    const seen = new Set<string>();
    return pool.filter((p) => p.image && p.stock > 0 && !seen.has(p.id) && seen.add(p.id)).slice(0, 5);
  });
  protected index = signal(0);
  protected paused = signal(false);
  protected added = signal(false);
  protected spot = computed(() => this.spotlight()[this.index() % Math.max(1, this.spotlight().length)] ?? null);
  protected spotOff = computed(() => (this.spot() ? discount(this.spot()!.price, this.spot()!.oldPrice) : null));

  constructor() {
    const reduce = typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (!reduce) {
      const timer = setInterval(() => {
        if (!this.paused() && this.spotlight().length > 1 && document.visibilityState === 'visible') {
          this.index.update((i) => (i + 1) % this.spotlight().length);
        }
      }, SPOTLIGHT_MS);
      inject(DestroyRef).onDestroy(() => clearInterval(timer));
    }
  }

  protected go(i: number) {
    this.index.set(i);
  }

  protected add(p: ProductSummary) {
    this.cart.add(p);
    this.added.set(true);
    setTimeout(() => this.added.set(false), 1500);
  }
}
