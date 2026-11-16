import { Injectable, signal } from '@angular/core';

export type Theme='light'|'dark';

@Injectable({providedIn:'root'})
export class ThemeService{
  private readonly key='asistencia.theme';
  readonly theme=signal<Theme>(this.initial());
  constructor(){this.apply(this.theme())}
  toggle(){const next=this.theme()==='light'?'dark':'light';this.theme.set(next);localStorage.setItem(this.key,next);this.apply(next)}
  private initial():Theme{const saved=localStorage.getItem(this.key);if(saved==='light'||saved==='dark')return saved;return matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light'}
  private apply(theme:Theme){document.documentElement.dataset['theme']=theme;document.documentElement.style.colorScheme=theme}
}
