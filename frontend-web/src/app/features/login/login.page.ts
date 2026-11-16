import { Component, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { AppIcon } from '../../shared/app-icon';

@Component({ imports: [ReactiveFormsModule, AppIcon], templateUrl: './login.page.html', styleUrl: './login.page.scss' })
export class LoginPage {
  readonly loading = signal(false); readonly error = signal('');
  form = new FormGroup({ username: new FormControl('', { nonNullable: true, validators: [Validators.required] }), password: new FormControl('', { nonNullable: true, validators: [Validators.required] }) });
  constructor(private auth: AuthService, private router: Router) {}
  submit() { if (this.loading() || this.form.invalid) return; this.loading.set(true); this.error.set(''); const credentials = this.form.getRawValue(); this.auth.login(credentials.username.trim(), credentials.password.trim()).subscribe({ next: () => this.router.navigateByUrl('/dashboard').then(navigated => { if (!navigated) { this.error.set('No fue posible abrir el panel.'); this.loading.set(false); } }), error: error => { this.error.set(error.status === 401 || error.status === 403 ? 'Usuario o contraseña incorrectos.' : 'No se pudo conectar con el servicio. Verifica tu conexión e inténtalo nuevamente.'); this.loading.set(false); } }); }
}
