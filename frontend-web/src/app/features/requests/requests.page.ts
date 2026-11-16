import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';

interface Row { id:number; type:string; affectedDate:string; reason:string; status:string; submittedAt:string }

@Component({
  imports:[FormsModule],
  template:`
    <div class="page-head"><div><h1>Solicitudes</h1><p>Permisos, justificaciones y correcciones con efecto sobre tu asistencia.</p></div></div>
    <form class="request-form" (ngSubmit)="submit()">
      <label>Tipo<select [(ngModel)]="type" name="type"><option value="JUSTIFICATION">Justificación</option><option value="PERMISSION">Permiso</option><option value="CORRECTION">Corrección</option></select></label>
      <label>Fecha afectada<input type="date" [(ngModel)]="affectedDate" name="affectedDate" required></label>
      @if(type==='PERMISSION'){
        <label>Desde<input type="datetime-local" [(ngModel)]="requestedFrom" name="requestedFrom" required></label>
        <label>Hasta<input type="datetime-local" [(ngModel)]="requestedTo" name="requestedTo" required></label>
      }
      @if(type==='CORRECTION'){
        <label>Marcación propuesta<select [(ngModel)]="proposedPunchType" name="proposedPunchType"><option value="ENTRY">Entrada</option><option value="EXIT">Salida</option></select></label>
        <label>Fecha y hora propuesta<input type="datetime-local" [(ngModel)]="proposedAt" name="proposedAt" required></label>
      }
      <label class="reason">Motivo<input [(ngModel)]="reason" name="reason" placeholder="Describe el motivo" required></label>
      <button [disabled]="saving()">{{saving()?'Enviando…':'Enviar solicitud'}}</button>
    </form>
    @if(message()){<div class="notice" [class.error]="failed()">{{message()}}</div>}
    <div class="table-wrap"><table><thead><tr><th>Fecha</th><th>Tipo</th><th>Motivo</th><th>Estado</th><th>Enviada</th></tr></thead><tbody>@for(r of rows();track r.id){<tr><td>{{r.affectedDate}}</td><td>{{translate(r.type)}}</td><td>{{r.reason}}</td><td><span class="badge">{{translate(r.status)}}</span></td><td>{{r.submittedAt}}</td></tr>}@empty{<tr><td colspan="5">Sin solicitudes</td></tr>}</tbody></table></div>`,
  styles:`.request-form{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:.8rem;margin-bottom:1rem;padding:1rem;background:var(--white);border:1px solid var(--line);border-radius:16px}.request-form label{display:grid;gap:.35rem;color:var(--blue);font-size:.8rem;font-weight:700}.request-form .reason{grid-column:1/-1}.request-form button{grid-column:1/-1}@media(max-width:700px){.request-form{grid-template-columns:1fr}.request-form .reason,.request-form button{grid-column:1}}`
})
export class RequestsPage implements OnInit {
  readonly rows=signal<Row[]>([]); readonly message=signal(''); readonly failed=signal(false); readonly saving=signal(false);
  type='JUSTIFICATION'; affectedDate=new Date().toISOString().slice(0,10); reason=''; requestedFrom=''; requestedTo=''; proposedPunchType='EXIT'; proposedAt='';
  constructor(private api:ApiService){}
  ngOnInit(){this.load()}
  load(){this.api.get<Row[]>('/requests/me').subscribe({next:r=>this.rows.set(r),error:()=>{this.failed.set(true);this.message.set('No se pudieron cargar tus solicitudes.')}})}
  submit(){if(!this.reason.trim()||!this.affectedDate)return;this.saving.set(true);const iso=(value:string)=>value?new Date(value).toISOString():null;this.api.post('/requests',{type:this.type,affectedDate:this.affectedDate,reason:this.reason.trim(),requestedFrom:this.type==='PERMISSION'?iso(this.requestedFrom):null,requestedTo:this.type==='PERMISSION'?iso(this.requestedTo):null,proposedPunchType:this.type==='CORRECTION'?this.proposedPunchType:null,proposedAt:this.type==='CORRECTION'?iso(this.proposedAt):null,requestedChange:null,evidenceReference:null}).subscribe({next:()=>{this.reason='';this.failed.set(false);this.message.set('Solicitud enviada.');this.saving.set(false);this.load()},error:e=>{this.failed.set(true);this.message.set(e.error?.detail??'No se pudo enviar.');this.saving.set(false)}})}
  translate(value:string){return ({JUSTIFICATION:'Justificación',PERMISSION:'Permiso',CORRECTION:'Corrección',SUBMITTED:'Pendiente',APPROVED:'Aprobada',REJECTED:'Rechazada'} as Record<string,string>)[value]??value}
}
