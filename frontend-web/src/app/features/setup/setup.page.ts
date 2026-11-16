import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api.service';

interface Item { id:number; code?:string; name?:string; firstNames?:string; lastNames?:string }
interface UserItem { id:number; username:string; roles:string[] }
interface Scope { id:number; user_id:number; username:string; site_id:number; site_name:string; area_id:number|null; area_name:string|null }

@Component({
  imports:[FormsModule],
  template:`
    <div class="page-head"><div><h1>Asignaciones</h1><p>Relaciona empleados con su organización y horario.</p></div><button (click)="load()">Actualizar</button></div>
    @if(message()){<div class="notice">{{message()}}</div>}
    <section class="setup-grid">
      <form class="setup-card" (ngSubmit)="assignOrganization()">
        <span class="step">01</span><h2>Ubicación y puesto</h2><p>Define dónde trabaja y qué función cumple el empleado.</p>
        <label>Empleado<select [(ngModel)]="organization.employeeId" name="employee" required><option [ngValue]="0" disabled>Seleccionar</option>@for(e of employees();track e.id){<option [ngValue]="e.id">{{employeeName(e)}}</option>}</select></label>
        <label>Sede<select [(ngModel)]="organization.siteId" name="site" required><option [ngValue]="0" disabled>Seleccionar</option>@for(x of sites();track x.id){<option [ngValue]="x.id">{{x.name}}</option>}</select></label>
        <label>Área<select [(ngModel)]="organization.areaId" name="area" required><option [ngValue]="0" disabled>Seleccionar</option>@for(x of areas();track x.id){<option [ngValue]="x.id">{{x.name}}</option>}</select></label>
        <label>Cargo<select [(ngModel)]="organization.positionId" name="position" required><option [ngValue]="0" disabled>Seleccionar</option>@for(x of positions();track x.id){<option [ngValue]="x.id">{{x.name}}</option>}</select></label>
        <label>Tipo de asignación<select [(ngModel)]="organization.assignmentType" name="assignmentType"><option value="PERMANENT">Permanente</option><option value="TEMPORARY">Temporal autorizada</option></select></label>
        <label>Vigente desde<input type="date" [(ngModel)]="organization.validFrom" name="orgFrom" required></label>
        @if(organization.assignmentType==='TEMPORARY'){<label>Vigente hasta<input type="date" [(ngModel)]="organization.validTo" name="orgTo" required></label><label>Motivo<input [(ngModel)]="organization.reason" name="assignmentReason" maxlength="500" required placeholder="Comisión, reemplazo o traslado temporal"></label>}
        <button [disabled]="saving()">Asignar organización</button>
      </form>
      <form class="setup-card" (ngSubmit)="assignSchedule()">
        <span class="step">02</span><h2>Horario vigente</h2><p>Asigna el horario usado para puntualidad y horas trabajadas.</p>
        <label>Empleado<select [(ngModel)]="schedule.employeeId" name="scheduleEmployee" required><option [ngValue]="0" disabled>Seleccionar</option>@for(e of employees();track e.id){<option [ngValue]="e.id">{{employeeName(e)}}</option>}</select></label>
        <label>Horario<select [(ngModel)]="schedule.scheduleId" name="schedule" required><option [ngValue]="0" disabled>Seleccionar</option>@for(x of schedules();track x.id){<option [ngValue]="x.id">{{x.name}}</option>}</select></label>
        <label>Vigente desde<input type="date" [(ngModel)]="schedule.validFrom" name="scheduleFrom" required></label>
        <button [disabled]="saving()">Asignar horario</button>
      </form>
      <form class="setup-card" (ngSubmit)="assignScope()">
        <span class="step">03</span><h2>Alcance del supervisor</h2><p>Limita empleados, solicitudes, incidencias y reportes por sede o área.</p>
        <label>Supervisor<select [(ngModel)]="scope.userId" name="scopeUser" required><option [ngValue]="0" disabled>Seleccionar</option>@for(u of supervisors();track u.id){<option [ngValue]="u.id">{{u.username}}</option>}</select></label>
        <label>Sede<select [(ngModel)]="scope.siteId" name="scopeSite" required><option [ngValue]="0" disabled>Seleccionar</option>@for(x of sites();track x.id){<option [ngValue]="x.id">{{x.name}}</option>}</select></label>
        <label>Área (opcional)<select [(ngModel)]="scope.areaId" name="scopeArea"><option [ngValue]="null">Toda la sede</option>@for(x of areas();track x.id){<option [ngValue]="x.id">{{x.name}}</option>}</select></label>
        <button [disabled]="saving()">Asignar alcance</button>
      </form>
    </section>`,
  styles:`.setup-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1rem}.setup-card{position:relative;display:grid;gap:.8rem;padding:1.35rem;background:var(--white);border:1px solid var(--line);border-radius:18px;box-shadow:var(--shadow-sm)}.setup-card h2{margin:0}.setup-card p{margin:-.4rem 0 .5rem;color:var(--muted)}.setup-card label{display:grid;gap:.4rem;color:var(--blue);font-size:.8rem;font-weight:700}.step{position:absolute;right:1.2rem;top:1.2rem;color:var(--accent);font-family:Manrope;font-weight:800}@media(max-width:760px){.setup-grid{grid-template-columns:1fr}}`
})
export class SetupPage implements OnInit {
  readonly employees=signal<Item[]>([]);readonly sites=signal<Item[]>([]);readonly areas=signal<Item[]>([]);readonly positions=signal<Item[]>([]);readonly schedules=signal<Item[]>([]);readonly users=signal<UserItem[]>([]);readonly supervisors=signal<UserItem[]>([]);readonly scopes=signal<Scope[]>([]);readonly message=signal('');readonly saving=signal(false);
  readonly today=new Date().toISOString().slice(0,10);
  organization={employeeId:0,siteId:0,areaId:0,positionId:0,validFrom:this.today,validTo:null as string|null,assignmentType:'PERMANENT',reason:''};
  schedule={employeeId:0,scheduleId:0,validFrom:this.today};
  scope={userId:0,siteId:0,areaId:null as number|null};
  constructor(private api:ApiService){}
  ngOnInit(){this.load()}
  load(){forkJoin({employees:this.api.get<Item[]>('/employees'),sites:this.api.get<Item[]>('/organization/sites'),areas:this.api.get<Item[]>('/organization/areas'),positions:this.api.get<Item[]>('/organization/positions'),schedules:this.api.get<Item[]>('/schedules'),users:this.api.get<UserItem[]>('/users'),scopes:this.api.get<Scope[]>('/supervisor-scopes')}).subscribe({next:data=>{this.employees.set(data.employees);this.sites.set(data.sites);this.areas.set(data.areas);this.positions.set(data.positions);this.schedules.set(data.schedules);this.users.set(data.users);this.supervisors.set(data.users.filter(u=>u.roles.includes('SUPERVISOR')));this.scopes.set(data.scopes)},error:()=>this.message.set('No se pudieron cargar los catálogos. Revisa usuarios, áreas, cargos, sedes y horarios.')})}
  employeeName(e:Item){return `${e.code??''} · ${e.firstNames??''} ${e.lastNames??''}`.trim()}
  assignOrganization(){if(!this.organization.employeeId||!this.organization.siteId||!this.organization.areaId||!this.organization.positionId)return;if(this.organization.assignmentType==='TEMPORARY'&&(!this.organization.validTo||!this.organization.reason.trim())){this.message.set('La asignación temporal requiere fecha de fin y motivo.');return}this.saving.set(true);this.api.post('/employee-assignments',{...this.organization,validTo:this.organization.assignmentType==='TEMPORARY'?this.organization.validTo:null,reason:this.organization.reason.trim()||null}).subscribe({next:()=>{this.message.set('Organización asignada correctamente.');this.saving.set(false)},error:e=>{this.message.set(e.error?.detail??'No se pudo crear la asignación.');this.saving.set(false)}})}
  assignSchedule(){if(!this.schedule.employeeId||!this.schedule.scheduleId)return;this.saving.set(true);this.api.post('/schedules/assignments',{...this.schedule,validTo:null}).subscribe({next:()=>{this.message.set('Horario asignado correctamente.');this.saving.set(false)},error:e=>{this.message.set(e.error?.detail??'No se pudo asignar el horario.');this.saving.set(false)}})}
  assignScope(){if(!this.scope.userId||!this.scope.siteId)return;this.saving.set(true);this.api.post('/supervisor-scopes',this.scope).subscribe({next:()=>{this.message.set('Alcance asignado correctamente.');this.saving.set(false);this.load()},error:e=>{this.message.set(e.error?.detail??'No se pudo asignar el alcance.');this.saving.set(false)}})}
}
