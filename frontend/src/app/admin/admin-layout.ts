import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth';
import { ThemeService } from '../core/theme';
import { Icon } from '../shared/icon';

@Component({
  selector: 'app-admin-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon],
  template: `
    <!-- Barre du haut : mobile uniquement -->
    <header class="topbar">
      <button type="button" class="icon-btn" aria-label="Ouvrir le menu" [attr.aria-expanded]="menuOpen()" aria-controls="admin-nav"
              (click)="menuOpen.set(true)">
        <app-icon name="menu" />
      </button>
      <a routerLink="/admin" class="logo"><span class="logo-mark" aria-hidden="true"></span><span>Tech<b>Store</b></span></a>
      <button type="button" class="icon-btn" (click)="theme.toggle()" [attr.aria-label]="theme.dark() ? 'Thème clair' : 'Thème sombre'">
        <app-icon [name]="theme.dark() ? 'sun' : 'moon'" [size]="20" />
      </button>
    </header>

    @if (menuOpen()) { <div class="backdrop drawer-backdrop" (click)="menuOpen.set(false)" aria-hidden="true"></div> }

    <div class="shell">
      <aside id="admin-nav" class="side" [class.open]="menuOpen()">
        <div class="side-head">
          <a routerLink="/admin" class="logo"><span class="logo-mark" aria-hidden="true"></span><span>Tech<b>Store</b></span></a>
          <button type="button" class="icon-btn close" aria-label="Fermer le menu" (click)="menuOpen.set(false)"><app-icon name="close" /></button>
        </div>
        <span class="role">Administration</span>
        <nav class="nav" aria-label="Administration" (click)="menuOpen.set(false)">
          <a routerLink="/admin" routerLinkActive="on" [routerLinkActiveOptions]="{ exact: true }"><app-icon name="dashboard" /> Tableau de bord</a>
          <a routerLink="/admin/produits" routerLinkActive="on"><app-icon name="box" /> Produits</a>
          <a routerLink="/admin/categories" routerLinkActive="on"><app-icon name="tag" /> Catégories</a>
          <a routerLink="/admin/commandes" routerLinkActive="on"><app-icon name="receipt" /> Commandes</a>
          <a routerLink="/admin/avis" routerLinkActive="on"><app-icon name="star" /> Avis clients</a>
          <a routerLink="/admin/codes-promo" routerLinkActive="on"><app-icon name="gift" /> Codes promo</a>
        </nav>
        <div class="side-foot">
          <button type="button" class="side-btn desk" (click)="theme.toggle()">
            <app-icon [name]="theme.dark() ? 'sun' : 'moon'" [size]="18" /> {{ theme.dark() ? 'Thème clair' : 'Thème sombre' }}
          </button>
          <a routerLink="/" target="_blank" class="side-btn"><app-icon name="store" [size]="18" /> Voir la boutique</a>
          <button type="button" class="side-btn" (click)="auth.logout()"><app-icon name="logout" [size]="18" /> Déconnexion</button>
          <p class="who">{{ auth.user()?.fullName || auth.user()?.email }}</p>
        </div>
      </aside>
      <main class="content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: `
    :host { display: block; min-height: 100vh; min-height: 100dvh; }
    .shell { display: grid; grid-template-columns: 264px minmax(0, 1fr); min-height: 100vh; min-height: 100dvh; }
    .topbar { display: none; }

    .side { position: sticky; top: 0; height: 100vh; height: 100dvh; overflow-y: auto; background: var(--night); color: var(--night-muted);
      padding: 22px 14px; display: flex; flex-direction: column; gap: 6px; }
    .side-head { display: flex; align-items: center; justify-content: space-between; padding: 0 8px; }
    .logo { display: inline-flex; align-items: center; gap: 10px; font-family: var(--font-display); font-weight: 500; font-size: 20px;
      letter-spacing: -.02em; color: #fff; text-decoration: none; }
    .logo b { font-weight: 700; color: #8fa6ff; }
    .logo-mark { width: 20px; height: 26px; border-radius: 999px 999px 4px 4px; background: #8fa6ff; position: relative; }
    .logo-mark::after { content: ''; position: absolute; left: 5px; right: 5px; bottom: 0; height: 13px; border-radius: 999px 999px 0 0; background: var(--night); }
    .close { display: none; color: #fff; }
    .role { font-size: 13px; padding: 2px 8px 14px; color: var(--night-muted); }
    .nav { display: flex; flex-direction: column; gap: 2px; }
    .nav a, .side-btn { display: flex; align-items: center; gap: 12px; padding: 11px 12px; border-radius: 12px; color: var(--night-muted);
      text-decoration: none; font: 500 15px var(--font-body); background: none; border: 0; cursor: pointer; text-align: left; width: 100%; }
    .nav a:hover, .side-btn:hover { background: var(--night-2); color: #fff; }
    .nav a.on { background: #8fa6ff; color: #071030; }
    .side-foot { margin-top: auto; padding-top: 16px; border-top: 1px solid var(--night-2); display: flex; flex-direction: column; gap: 2px; }
    .side-btn { font-size: 14px; padding: 9px 12px; }
    .who { padding: 10px 12px 0; font-size: 13px; color: var(--night-muted); word-break: break-all; }
    .content { min-width: 0; padding: 28px clamp(16px, 3vw, 40px) 48px; }

    @media (max-width: 900px) {
      .shell { grid-template-columns: 1fr; }
      .topbar { display: flex; align-items: center; justify-content: space-between; position: sticky; top: 0; z-index: 40; padding: 8px 8px;
        background: var(--night); color: #fff; }
      .side { position: fixed; z-index: 70; top: 0; left: 0; bottom: 0; width: min(300px, 86vw); transform: translateX(-100%);
        visibility: hidden; transition: transform .28s cubic-bezier(.2,.8,.2,1), visibility .28s; box-shadow: var(--shadow-lift); }
      .side.open { transform: none; visibility: visible; }
      .close { display: inline-flex; }
      .desk { display: none; }
      .content { padding-top: 20px; }
    }
  `,
})
export class AdminLayout {
  protected auth = inject(AuthService);
  protected theme = inject(ThemeService);
  protected menuOpen = signal(false);
}
