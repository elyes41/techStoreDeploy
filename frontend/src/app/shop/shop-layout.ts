import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { catchError, of } from 'rxjs';
import { ApiService } from '../core/api.service';
import { CartService } from '../core/cart.service';
import { WishlistService } from '../core/stores';
import { ThemeService } from '../core/theme';
import { Icon } from '../shared/icon';
import { CompareBar } from './compare-bar';
import { SearchBox } from './search-box';

@Component({
  selector: 'app-shop-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon, SearchBox, CompareBar],
  template: `
    <a class="skip" href="#contenu">Aller au contenu</a>
    <div class="topbar">Livraison dans les 24 gouvernorats, paiement à la livraison</div>

    <header class="header">
      <div class="container header-main">
        <a routerLink="/" class="logo" aria-label="TechStore, accueil">
          <span class="logo-mark" aria-hidden="true"></span><span>Tech<b>Store</b></span>
        </a>
        <app-search-box class="search" />
        <div class="actions">
          <button type="button" class="icon-btn" (click)="theme.toggle()"
                  [attr.aria-label]="theme.dark() ? 'Passer en thème clair' : 'Passer en thème sombre'">
            <app-icon [name]="theme.dark() ? 'sun' : 'moon'" [size]="20" />
          </button>
          <a routerLink="/favoris" class="action desk" routerLinkActive="on">
            <app-icon name="heart" [size]="20" /><span>Favoris</span>
            @if (wishlist.count() > 0) { <span class="count">{{ wishlist.count() }}</span> }
          </a>
          <a routerLink="/panier" class="action cart" routerLinkActive="on" [attr.aria-label]="'Panier, ' + cart.count() + ' article(s)'">
            <app-icon name="cart" [size]="20" /><span class="desk-label">Panier</span>
            @if (cart.count() > 0) { <span class="count">{{ cart.count() }}</span> }
          </a>
        </div>
      </div>
      <nav class="container cats" aria-label="Rayons">
        <a routerLink="/catalogue" routerLinkActive="on" [routerLinkActiveOptions]="{ exact: true }">Tout</a>
        @for (c of categories(); track c.id) {
          <a [routerLink]="['/catalogue', c.slug]" routerLinkActive="on">{{ c.name }}</a>
        }
        <a routerLink="/catalogue" [queryParams]="{ promo: true }" class="promo">Promos</a>
      </nav>
    </header>

    <main id="contenu">
      <router-outlet />
    </main>
    <app-compare-bar />

    <footer class="footer">
      <div class="container footer-top">
        <div class="brand">
          <span class="logo"><span class="logo-mark" aria-hidden="true"></span><span>Tech<b>Store</b></span></span>
          <p>La tech au juste prix, livrée partout en Tunisie.</p>
          <p class="muted-n">[Adresse du showroom], Tunis<br />[Téléphone] – [WhatsApp]</p>
        </div>
        <nav class="col" aria-label="Rayons">
          <strong>Rayons</strong>
          @for (c of categories().slice(0, 8); track c.id) { <a [routerLink]="['/catalogue', c.slug]">{{ c.name }}</a> }
        </nav>
        <div class="col">
          <strong>Aide</strong>
          <span>Livraison sous [délai]</span>
          <span>Retour sous [délai]</span>
        </div>
        <div class="col">
          <strong>Paiement</strong>
          <span>À la livraison</span>
          <span>Carte bancaire et e-Dinar</span>
          <span>Portefeuille mobile</span>
        </div>
      </div>
      <div class="container footer-bottom">© {{ year }} TechStore</div>
    </footer>

    <!-- Barre d'onglets : mobile uniquement -->
    <nav class="tabbar" aria-label="Navigation principale">
      <a routerLink="/" routerLinkActive="on" [routerLinkActiveOptions]="{ exact: true }"><app-icon name="home" [size]="22" />Accueil</a>
      <a routerLink="/catalogue" routerLinkActive="on"><app-icon name="grid" [size]="22" />Catalogue</a>
      <a routerLink="/favoris" routerLinkActive="on">
        <span class="tab-icon"><app-icon name="heart" [size]="22" />@if (wishlist.count() > 0) { <span class="dot">{{ wishlist.count() }}</span> }</span>Favoris
      </a>
      <a routerLink="/panier" routerLinkActive="on">
        <span class="tab-icon"><app-icon name="cart" [size]="22" />@if (cart.count() > 0) { <span class="dot">{{ cart.count() }}</span> }</span>Panier
      </a>
    </nav>
  `,
  styles: `
    :host { display: flex; flex-direction: column; min-height: 100vh; min-height: 100dvh; }
    main { flex: 1; }
    .skip { position: absolute; left: -9999px; top: 8px; z-index: 100; background: var(--surface); padding: 8px 14px; border-radius: 999px; }
    .skip:focus { left: 8px; }

    .topbar { background: var(--blue); color: var(--on-blue); font-size: 13px; font-weight: 500; text-align: center; padding: 7px 16px; }

    .header { position: sticky; top: 0; z-index: 40; background: var(--glass); backdrop-filter: saturate(1.6) blur(18px);
      -webkit-backdrop-filter: saturate(1.6) blur(18px); border-bottom: 1px solid var(--line); }
    .header-main { display: flex; align-items: center; gap: 20px; padding-top: 12px; padding-bottom: 10px; }
    .logo { display: inline-flex; align-items: center; gap: 10px; font-family: var(--font-display); font-weight: 500; font-size: 21px;
      letter-spacing: -.03em; color: var(--ink); text-decoration: none; flex: none; }
    .logo b { font-weight: 700; color: var(--blue); }
    /* l'arche, en petit : le repère de la marque */
    .logo-mark { width: 22px; height: 28px; border-radius: 999px 999px 4px 4px; background: var(--blue); position: relative; }
    .logo-mark::after { content: ''; position: absolute; left: 6px; right: 6px; bottom: 0; height: 14px; border-radius: 999px 999px 0 0; background: var(--bg); }
    .search { flex: 1 1 auto; max-width: 620px; }
    .actions { display: flex; align-items: center; gap: 4px; margin-left: auto; color: var(--ink); }
    .action { position: relative; display: inline-flex; align-items: center; gap: 8px; height: 44px; padding: 0 14px; border-radius: 999px;
      color: var(--ink); text-decoration: none; font-weight: 500; font-size: 15px; }
    .action:hover, .action.on { background: var(--surface-2); color: var(--ink); }
    .cart { background: var(--ink); color: var(--bg); }
    .cart:hover, .cart.on { background: var(--blue); color: var(--on-blue); }
    .count { min-width: 20px; height: 20px; padding: 0 6px; border-radius: 999px; background: var(--bloom); color: #fff;
      font-size: 12px; font-weight: 700; display: inline-flex; align-items: center; justify-content: center; }

    .cats { display: flex; gap: 6px; overflow-x: auto; scrollbar-width: none; padding-bottom: 10px; -webkit-mask-image: linear-gradient(90deg, #000 92%, transparent); mask-image: linear-gradient(90deg, #000 92%, transparent); }
    .cats::-webkit-scrollbar { display: none; }
    .cats a { flex: none; color: var(--ink-2); text-decoration: none; font-size: 14px; font-weight: 500; padding: 7px 14px; border-radius: 999px;
      border: 1px solid var(--line); background: var(--surface); transition: background .15s, color .15s, border-color .15s; }
    .cats a:hover { border-color: var(--ink-2); color: var(--ink); }
    .cats a.on { background: var(--ink); border-color: var(--ink); color: var(--bg); }
    .cats .promo { color: var(--bloom); border-color: color-mix(in srgb, var(--bloom) 40%, transparent); font-weight: 600; }

    .footer { background: var(--night); color: var(--night-muted); font-size: 14px; margin-top: 72px;
      border-radius: 32px 32px 0 0; padding-bottom: var(--tabbar-h); }
    .footer .logo { color: #fff; }
    .footer .logo b { color: #8fa6ff; }
    .footer .logo-mark { background: #8fa6ff; }
    .footer .logo-mark::after { background: var(--night); }
    .footer-top { display: grid; grid-template-columns: 1.6fr repeat(3, 1fr); gap: 32px; padding-top: 56px; padding-bottom: 32px; }
    .brand { display: flex; flex-direction: column; gap: 12px; max-width: 34ch; }
    .brand p:first-of-type { color: var(--night-ink); font-size: 16px; }
    .col { display: flex; flex-direction: column; gap: 10px; }
    .col strong { color: #fff; font-weight: 600; }
    .footer a { color: var(--night-muted); text-decoration: none; }
    .footer a:hover { color: #fff; }
    .footer-bottom { border-top: 1px solid var(--night-2); padding-top: 18px; padding-bottom: 24px; font-size: 13px; }

    .tabbar { display: none; }

    @media (max-width: 900px) {
      .footer-top { grid-template-columns: 1fr 1fr; }
      .brand { grid-column: 1 / -1; }
    }

    /* ---------- Mobile ---------- */
    @media (max-width: 760px) {
      :host { --tabbar-h: calc(64px + env(safe-area-inset-bottom)); }
      .topbar { font-size: 12px; padding: 6px 12px; }
      .header-main { flex-wrap: wrap; gap: 10px; padding-top: 10px; }
      .logo { font-size: 19px; }
      .search { order: 3; flex-basis: 100%; max-width: none; }
      .desk, .desk-label { display: none; }
      .cart { width: 44px; padding: 0; justify-content: center; }
      .cart .count { position: absolute; top: -2px; right: -4px; }
      .footer { margin-top: 48px; border-radius: 24px 24px 0 0; }
      .footer-top { padding-top: 40px; gap: 24px; }

      .tabbar { display: grid; grid-template-columns: repeat(4, 1fr); position: fixed; left: 0; right: 0; bottom: 0; z-index: 45;
        height: var(--tabbar-h); padding: 6px 8px env(safe-area-inset-bottom); background: var(--glass);
        backdrop-filter: saturate(1.6) blur(18px); -webkit-backdrop-filter: saturate(1.6) blur(18px); border-top: 1px solid var(--line); }
      .tabbar a { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px; font-size: 11px; font-weight: 600;
        color: var(--muted); text-decoration: none; border-radius: 16px; }
      .tabbar a.on { color: var(--blue); }
      .tabbar a.on app-icon { transform: translateY(-1px); }
      .tab-icon { position: relative; display: inline-flex; }
      .dot { position: absolute; top: -4px; right: -10px; min-width: 18px; height: 18px; padding: 0 5px; border-radius: 999px;
        background: var(--bloom); color: #fff; font-size: 11px; display: inline-flex; align-items: center; justify-content: center; }
    }
  `,
})
export class ShopLayout {
  private api = inject(ApiService);
  protected cart = inject(CartService);
  protected wishlist = inject(WishlistService);
  protected theme = inject(ThemeService);
  protected categories = toSignal(this.api.categories().pipe(catchError(() => of([]))), { initialValue: [] });
  protected year = new Date().getFullYear();
}
