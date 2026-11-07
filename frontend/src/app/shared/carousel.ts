import { ChangeDetectionStrategy, Component, ElementRef, input, viewChild } from '@angular/core';
import { ProductSummary } from '../core/models';
import { Icon } from './icon';
import { ProductCard } from './product-card';

/** Rangée horizontale de produits défilante (style Amazon), avec flèches sur grand écran. */
@Component({
  selector: 'app-product-carousel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ProductCard, Icon],
  template: `
    @if (products().length) {
      <section class="carousel" [attr.aria-label]="title()">
        <div class="head">
          <div class="stack" style="gap: 2px">
            <h2>{{ title() }}</h2>
            @if (subtitle()) { <p class="muted small">{{ subtitle() }}</p> }
          </div>
          <div class="arrows">
            <button type="button" class="nav-btn" aria-label="Précédents" (click)="scroll(-1)"><app-icon name="chevronLeft" [size]="18" /></button>
            <button type="button" class="nav-btn" aria-label="Suivants" (click)="scroll(1)"><app-icon name="chevronRight" [size]="18" /></button>
          </div>
        </div>
        <div class="track" #track>
          @for (p of products(); track p.id) {
            <div class="slot"><app-product-card [product]="p" /></div>
          }
        </div>
      </section>
    }
  `,
  styles: `
    .carousel { display: flex; flex-direction: column; gap: 16px; }
    .head { display: flex; justify-content: space-between; align-items: flex-end; gap: 12px; }
    .arrows { display: flex; gap: 8px; }
    .nav-btn { width: 40px; height: 40px; border-radius: 999px; border: 1px solid var(--line-2); background: var(--surface); color: var(--ink);
      display: inline-flex; align-items: center; justify-content: center; cursor: pointer; transition: border-color .15s, background .15s; }
    .nav-btn:hover { border-color: var(--ink); }
    /* défilement bord à bord sur mobile : la rangée déborde du conteneur */
    .track { display: grid; grid-auto-flow: column; grid-auto-columns: minmax(236px, 250px); gap: 16px;
      overflow-x: auto; scroll-snap-type: x mandatory; padding: 2px 2px 10px; scrollbar-width: none; overscroll-behavior-x: contain; }
    .track::-webkit-scrollbar { display: none; }
    .slot { scroll-snap-align: start; }
    @media (max-width: 640px) {
      .arrows { display: none; }
      .track { grid-auto-columns: 46%; gap: 12px; margin: 0 calc(-1 * clamp(16px, 3vw, 28px)); padding-left: clamp(16px, 3vw, 28px);
        padding-right: clamp(16px, 3vw, 28px); scroll-padding-left: clamp(16px, 3vw, 28px); }
    }
  `,
})
export class ProductCarousel {
  title = input.required<string>();
  subtitle = input<string | null>(null);
  products = input<ProductSummary[]>([]);
  private track = viewChild<ElementRef<HTMLElement>>('track');

  scroll(dir: number) {
    const el = this.track()?.nativeElement;
    if (el) el.scrollBy({ left: dir * el.clientWidth * 0.8, behavior: 'smooth' });
  }
}
