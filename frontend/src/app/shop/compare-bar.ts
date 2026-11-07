import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';
import { CompareService } from '../core/stores';
import { Icon } from '../shared/icon';

/** Barre flottante du comparateur, visible dès qu'un produit y est ajouté. */
@Component({
  selector: 'app-compare-bar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon],
  template: `
    @if (compare.count() > 0 && !onComparePage()) {
      <div class="spacer-bar" aria-hidden="true"></div>
      <aside class="bar" aria-label="Comparateur">
        <div class="items">
          @for (p of compare.items(); track p.id) {
            <span class="item">
              <span class="name">{{ p.name }}</span>
              <button type="button" class="icon-btn" [attr.aria-label]="'Retirer ' + p.name" (click)="compare.remove(p.id)"><app-icon name="close" [size]="14" /></button>
            </span>
          }
        </div>
        <div class="row" style="gap: 8px">
          <button type="button" class="btn btn-outline btn-sm" (click)="compare.clear()">Vider</button>
          <a routerLink="/comparer" class="btn btn-primary btn-sm" [class.disabled]="compare.count() < 2">
            Comparer ({{ compare.count() }})
          </a>
        </div>
      </aside>
    }
  `,
  styles: `
    .spacer-bar { height: 96px; }
    .bar { position: fixed; left: 50%; bottom: calc(var(--tabbar-h) + 12px); transform: translateX(-50%); z-index: 30;
      width: min(960px, calc(100% - 24px)); background: var(--glass); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px);
      color: var(--ink); border: 1px solid var(--line); border-radius: var(--radius); padding: 10px 12px; display: flex; flex-wrap: wrap; gap: 10px;
      align-items: center; justify-content: space-between; box-shadow: var(--shadow-lift); }
    .items { display: flex; gap: 8px; overflow-x: auto; min-width: 0; flex: 1 1 260px; scrollbar-width: none; }
    .item { display: inline-flex; align-items: center; gap: 2px; background: var(--surface-2); border-radius: 999px; padding-left: 12px;
      font-size: 13px; max-width: 200px; flex: none; }
    .name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .item .icon-btn { width: 32px; height: 32px; color: var(--muted); }
    .disabled { opacity: .5; pointer-events: none; }
  `,
})
export class CompareBar {
  protected compare = inject(CompareService);
  private router = inject(Router);
  // Masquée sur la page du comparateur elle-même
  protected onComparePage = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map(() => this.router.url.startsWith('/comparer')),
      startWith(this.router.url.startsWith('/comparer')),
    ),
    { initialValue: false },
  );
}
