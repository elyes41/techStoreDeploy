import { Injectable, computed, signal } from '@angular/core';

export type ThemeChoice = 'system' | 'light' | 'dark';
const KEY = 'techstore.theme';

/** Thème clair / sombre : suit le réglage du téléphone, ou le choix du visiteur (mémorisé). */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private media = typeof matchMedia === 'function' ? matchMedia('(prefers-color-scheme: dark)') : null;
  private systemDark = signal(this.media?.matches ?? false);
  readonly choice = signal<ThemeChoice>(read());
  readonly dark = computed(() => (this.choice() === 'system' ? this.systemDark() : this.choice() === 'dark'));

  constructor() {
    this.media?.addEventListener('change', (e) => this.systemDark.set(e.matches));
  }

  /** Bascule clair <-> sombre (le choix devient explicite). */
  toggle() {
    this.set(this.dark() ? 'light' : 'dark');
  }

  set(choice: ThemeChoice) {
    this.choice.set(choice);
    const root = document.documentElement;
    if (choice === 'system') delete root.dataset['theme'];
    else root.dataset['theme'] = choice;
    try {
      if (choice === 'system') localStorage.removeItem(KEY);
      else localStorage.setItem(KEY, choice);
    } catch {
      /* stockage indisponible : le choix vaut pour cette visite */
    }
  }
}

function read(): ThemeChoice {
  try {
    const v = localStorage.getItem(KEY);
    return v === 'light' || v === 'dark' ? v : 'system';
  } catch {
    return 'system';
  }
}
