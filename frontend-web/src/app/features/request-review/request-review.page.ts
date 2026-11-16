import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { ApiService } from '../../core/api.service';

interface RequestRow {
  id:number; employeeId:number; employeeCode:string; employeeName:string; employeePhotoUrl:string|null;
  type:string; affectedDate:string; reason:string; status:string; submittedAt:string;
  requestedFrom:string|null; requestedTo:string|null; proposedPunchType:string|null; proposedAt:string|null;
}

@Component({
  imports:[FormsModule,DatePipe],
  template:`
    <div class="page-head"><div><h1>Revisar solicitudes</h1><p>Evalúa el sustento y el efecto que se aplicará a la asistencia.</p></div><button class="secondary" (click)="load()">Actualizar</button></div>
    @if(message()){<div class="notice" [class.error]="failed()">{{message()}}</div>}
    <div class="request-list">
      @for(row of rows();track row.id){<article class="request-card">
        <header><div class="person"><div class="avatar">@if(row.employeePhotoUrl){<img [src]="row.employeePhotoUrl" [alt]="row.employeeName">}@else{<span>{{initials(row.employeeName)}}</span>}</div><div><h2>{{row.employeeName}}</h2><span>{{row.employeeCode}} · Solicitud #{{row.id}}</span></div></div><span class="badge">{{typeLabel(row.type)}}</span></header>
        <div class="request-body"><div><small>Fecha afectada</small><strong>{{row.affectedDate}}</strong></div><div><small>Detalle solicitado</small><strong>{{detail(row)}}</strong></div><div class="reason"><small>Motivo</small><p>{{row.reason}}</p></div></div>
        <label>Comentario de revisión<textarea [(ngModel)]="comments[row.id]" [name]="'comment'+row.id" rows="2" placeholder="Obligatorio al rechazar; recomendado al aprobar"></textarea></label>
        <footer><span>Enviada {{row.submittedAt | date:'dd/MM/yyyy HH:mm'}}</span><div class="actions"><button (click)="review(row,'APPROVED')" [disabled]="processing()===row.id">{{processing()===row.id?'Procesando…':'Aprobar'}}</button><button class="secondary" (click)="review(row,'REJECTED')" [disabled]="processing()===row.id">Rechazar</button></div></footer>
      </article>}@empty{<div class="empty-state">No hay solicitudes pendientes.</div>}
    </div>`,
  styles:`.request-list{display:grid;gap:1rem}.request-card{display:grid;gap:1rem;padding:1.2rem;background:var(--white);border:1px solid var(--line);border-radius:18px;box-shadow:var(--shadow-sm)}header,footer,.person,.actions{display:flex;align-items:center}header,footer{justify-content:space-between;gap:1rem}.person{gap:.75rem}.person h2{margin:0;font-size:1rem}.person span,footer>span{color:var(--muted);font-size:.75rem}.avatar{display:grid;place-items:center;width:44px;height:44px;overflow:hidden;border-radius:13px;background:linear-gradient(145deg,var(--sky),var(--accent));color:white;font-weight:800}.avatar img{width:100%;height:100%;object-fit:cover}.request-body{display:grid;grid-template-columns:repeat(2,1fr);gap:.8rem;padding:.9rem;background:var(--surface);border-radius:13px}.request-body>div{display:grid;gap:.25rem}.request-body small,label{color:var(--muted);font-size:.75rem}.reason{grid-column:1/-1}.reason p{margin:0}.request-card label{display:grid;gap:.35rem;font-weight:700}.actions{gap:.5rem}@media(max-width:650px){header,footer{align-items:flex-start;flex-direction:column}.request-body{grid-template-columns:1fr}.actions{width:100%}.actions button{flex:1}}`
})
export class RequestReviewPage implements OnInit {
  readonly rows=signal<RequestRow[]>([]); readonly message=signal(''); readonly failed=signal(false); readonly processing=signal<number|null>(null);
  readonly comments:Record<number,string>={};
  constructor(private api:ApiService){}
  ngOnInit(){this.load()}
  load(){this.api.get<RequestRow[]>('/requests',{status:'SUBMITTED'}).subscribe({next:r=>this.rows.set(r),error:()=>{this.failed.set(true);this.message.set('No se pudieron cargar las solicitudes.')}})}
  review(row:RequestRow,decision:'APPROVED'|'REJECTED'){const comment=(this.comments[row.id]??'').trim();if(decision==='REJECTED'&&!comment){this.failed.set(true);this.message.set('Debes indicar el motivo del rechazo.');return}if(this.processing()!==null)return;this.processing.set(row.id);this.failed.set(false);this.message.set('');this.api.patch(`/requests/${row.id}/review`,{decision,comment}).subscribe({next:()=>{this.processing.set(null);this.message.set(decision==='APPROVED'?'Solicitud aprobada y efecto aplicado.':'Solicitud rechazada.');this.load()},error:error=>{this.processing.set(null);this.failed.set(true);this.message.set(error.error?.detail??'No se pudo completar la revisión.')}})}
  initials(name:string){return name.split(/\s+/).slice(0,2).map(x=>x[0]).join('').toUpperCase()}
  typeLabel(type:string){return ({JUSTIFICATION:'Justificación',PERMISSION:'Permiso',CORRECTION:'Corrección'} as Record<string,string>)[type]??type}
  detail(row:RequestRow){if(row.type==='CORRECTION')return `${row.proposedPunchType==='ENTRY'?'Entrada':'Salida'} · ${row.proposedAt??'sin hora'}`;if(row.type==='PERMISSION')return `${row.requestedFrom??'—'} a ${row.requestedTo??'—'}`;return 'Justificación de ausencia'}
}
