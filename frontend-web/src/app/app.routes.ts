import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/auth.guard';
export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.page').then((m) => m.LoginPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.page').then((m) => m.DashboardPage),
      },
      {
        path: 'attendance',
        loadComponent: () =>
          import('./features/attendance/attendance.page').then((m) => m.AttendancePage),
      },
      {
        path: 'requests',
        loadComponent: () =>
          import('./features/requests/requests.page').then((m) => m.RequestsPage),
      },
      {
        path: 'request-review',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'SUPERVISOR'] },
        loadComponent: () =>
          import('./features/request-review/request-review.page').then((m) => m.RequestReviewPage),
      },
      {
        path: 'attendance-contingency',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'SUPERVISOR'] },
        loadComponent: () =>
          import('./features/attendance-contingency/attendance-contingency.page').then(
            (m) => m.AttendanceContingencyPage,
          ),
      },
      {
        path: 'attendance-security',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () =>
          import('./features/attendance-security/attendance-security.page').then(
            (m) => m.AttendanceSecurityPage,
          ),
      },
      {
        path: 'setup',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () => import('./features/setup/setup.page').then((m) => m.SetupPage),
      },
      {
        path: 'users',
        canActivate: [roleGuard],
        data: {
          roles: ['ADMIN'],
          title: 'Usuarios',
          endpoint: '/users',
          userResource: true,
          fields: [
            ['username', 'Usuario'],
            ['password', 'Contraseña', 'password'],
            ['photoUrl', 'URL de foto', 'url'],
            ['roles', 'Roles (ADMIN, SUPERVISOR, EMPLOYEE)'],
          ],
        },
        loadComponent: () =>
          import('./features/resource/resource.page').then((m) => m.ResourcePage),
      },
      {
        path: 'employees',
        canActivate: [roleGuard],
        data: {
          roles: ['ADMIN', 'SUPERVISOR'],
          title: 'Empleados',
          endpoint: '/employees',
          fields: [
            ['code', 'Código'],
            ['documentType', 'Tipo documento'],
            ['documentNumber', 'Documento'],
            ['firstNames', 'Nombres'],
            ['lastNames', 'Apellidos'],
            ['userId', 'ID del usuario', 'number'],
            ['photoUrl', 'URL de foto', 'url'],
            ['email', 'Correo', 'email'],
            ['phone', 'Teléfono'],
            ['hireDate', 'Ingreso', 'date'],
          ],
        },
        loadComponent: () =>
          import('./features/resource/resource.page').then((m) => m.ResourcePage),
      },
      {
        path: 'sites',
        canActivate: [roleGuard],
        data: {
          roles: ['ADMIN'],
          title: 'Sedes',
          endpoint: '/organization/sites',
          fields: [
            ['code', 'Código'],
            ['name', 'Nombre'],
            ['address', 'Dirección'],
            ['timeZone', 'Zona horaria'],
            ['latitude', 'Latitud', 'number'],
            ['longitude', 'Longitud', 'number'],
          ],
        },
        loadComponent: () =>
          import('./features/resource/resource.page').then((m) => m.ResourcePage),
      },
      {
        path: 'areas',
        canActivate: [roleGuard],
        data: {
          roles: ['ADMIN'],
          title: 'Áreas',
          endpoint: '/organization/areas',
          fields: [
            ['code', 'Código'],
            ['name', 'Nombre'],
            ['description', 'Descripción'],
          ],
        },
        loadComponent: () =>
          import('./features/resource/resource.page').then((m) => m.ResourcePage),
      },
      {
        path: 'positions',
        canActivate: [roleGuard],
        data: {
          roles: ['ADMIN'],
          title: 'Cargos',
          endpoint: '/organization/positions',
          fields: [
            ['code', 'Código'],
            ['name', 'Nombre'],
            ['description', 'Descripción'],
          ],
        },
        loadComponent: () =>
          import('./features/resource/resource.page').then((m) => m.ResourcePage),
      },
      {
        path: 'schedules',
        canActivate: [roleGuard],
        data: {
          roles: ['ADMIN', 'SUPERVISOR'],
          title: 'Horarios',
          endpoint: '/schedules',
          schedule: true,
          fields: [
            ['code', 'Código'],
            ['name', 'Nombre'],
            ['description', 'Descripción'],
            ['startTime', 'Entrada', 'time'],
            ['endTime', 'Salida', 'time'],
            ['breakMinutes', 'Descanso (min)', 'number'],
          ],
        },
        loadComponent: () =>
          import('./features/resource/resource.page').then((m) => m.ResourcePage),
      },
      {
        path: 'incidents',
        canActivate: [roleGuard],
        data: {
          roles: ['ADMIN', 'SUPERVISOR'],
          title: 'Incidencias',
          endpoint: '/incidents',
          fields: [
            ['employeeId', 'ID empleado', 'number'],
            ['type', 'Tipo'],
            ['description', 'Descripción'],
            ['occurredOn', 'Fecha', 'date'],
          ],
        },
        loadComponent: () =>
          import('./features/resource/resource.page').then((m) => m.ResourcePage),
      },
      {
        path: 'reports',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'SUPERVISOR'] },
        loadComponent: () => import('./features/reports/reports.page').then((m) => m.ReportsPage),
      },
      {
        path: 'audit',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () => import('./features/audit/audit.page').then((m) => m.AuditPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
