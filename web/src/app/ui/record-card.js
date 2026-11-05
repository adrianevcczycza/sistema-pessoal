"use client";
import {Heart,CalendarDays,Check,ExternalLink,Plus,Copy,Clock3,Download,Pencil,Trash2} from 'lucide-react';
import {money,shortDate} from './components';
import {api,saveRecord,downloadBlob} from '../../lib/api';
export default function RecordCard({module,record,today,edit,remove,toggle,busy,action,purchase,execute,ingredients,showHistory,selected,select}){
 const d=record.data;const isLate=!d.done&&(d.dueDate||d.date)&&(d.dueDate||d.date)<today;
 return <article className={`record-card ${d.done?'completed':''}`}>
 {['tasks','groceries'].includes(module)&&<input type="checkbox" checked={!!d.done} onChange={toggle} disabled={busy} aria-label={`Concluir ou reabrir ${d.title}`}/>}
 {module==='house'&&<input type="checkbox" checked={!!selected} onChange={select} aria-label={`Selecionar ${d.title}`}/>}
 <div className="record-body"><button className="record-title" onClick={edit}>{d.title}{d.favorite&&<Heart size={14}/>}</button><div className="record-meta">{[d.room,d.category,d.notebook,d.subject,d.institution,d.store].filter(Boolean).map((v,i)=><span key={i}>{v}</span>)}{(d.date||d.deadline||d.dueDate)&&<span><CalendarDays size={12}/>{shortDate(d.dueDate||d.deadline||d.date)}</span>}{d.priority&&<span className="pill">{d.priority}</span>}{d.done&&<span className="pill green"><Check size={11}/> Concluída</span>}{module==='tasks'&&isLate&&<span className="pill warning">Prazo vencido</span>}{d.status&&<span className="pill">{d.status}</span>}</div>
 {d.description&&<p className="record-description">{d.description}</p>}
 {module==='house'&&<><div className="house-detail"><span>Precisamos: <strong>{d.needed}</strong></span><span>Disponíveis: <strong>{d.available||0}</strong></span><span>Faltam: <strong>{d.missing||0}</strong></span><span>Preço-alvo: <strong>{money(d.price)}</strong></span><span className={`pill ${d.resolved?'green':''}`}>{d.notNeeded?'Não precisamos':d.resolved?'Resolvido':d.purchaseStatus}</span></div><div className="record-links">{[['magalu','Magalu'],['mercadoLivre','Mercado Livre'],['amazon','Amazon']].map(([key,label])=>d[key]&&/^https?:\/\//.test(d[key])?<a key={key} href={d[key]} target="_blank" rel="noopener noreferrer">Ver na {label}<ExternalLink size={12}/></a>:null)}</div></>}
 {module==='transactions'&&<p className="record-value">{money(d.amount)} <span className={`pill ${d.paid?'green':''}`}>{d.type} · {d.paid?'Realizado':'Previsto'}</span>{d.purchaseId&&<small>Vinculada a uma compra da casa</small>}</p>}
 {module==='transactions'&&d.recurrence==='Mensal'&&!d.purchaseId&&<button className="small-button" disabled={busy} onClick={()=>action(()=>api(`/finance/recurring/${record.id}/next`,{method:'POST'}),'Próximo mês criado sem duplicação.')}>Gerar próximo mês</button>}{module==='investments'&&<p className="record-value">{money(d.amount)} <span className="pill">{d.movement} · Manual</span></p>}
 {module==='purchases'&&<p>{d.quantity} unidade(s) · {money(d.amount)} + {money(d.freight||0)} de frete · {d.received?'Recebido':'Entrega não confirmada'}</p>}
 {module==='comparisons'&&<p className="record-value">Custo total: {money(d.total)} <small>Quantidade × preço unitário + frete</small></p>}{module==='water'&&<p>{d.ml} ml</p>}{module==='notes'&&<p className="note-excerpt">{(d.content||'').slice(0,160)}</p>}
 {module==='projects'&&d.checklist&&<p className="muted">Etapas: {d.completedSteps||0} de {d.totalSteps||0} concluídas · {d.progress||0}%</p>}
 </div><div className="record-actions">
 {module==='house'&&!d.notNeeded&&Number(d.missing)>0&&<><button className="small-button" disabled={busy} onClick={()=>action(()=>saveRecord('house',{...d,shopping:true},record),'Item na lista de compras.')}><Plus size={13}/>{d.shopping?'Nas compras':'Às compras'}</button><button className="small-button accent" onClick={purchase}>Registrar compra</button></>}
 {module==='workouts'&&<button className="small-button accent" onClick={execute}>Registrar sessão</button>}
 {module==='recipes'&&<button className="small-button" disabled={busy} onClick={ingredients}>Comprar ingredientes</button>}
 {module==='prompts'&&<button className="icon-button" aria-label="Copiar prompt" title="Copiar prompt" onClick={()=>action(()=>navigator.clipboard.writeText(d.content||''),'Prompt copiado.')}><Copy size={16}/></button>}
 {['prompts','notes'].includes(module)&&<button className="icon-button" title="Histórico de versões" aria-label="Histórico de versões" onClick={showHistory}><Clock3 size={16}/></button>}
 {module==='notes'&&<button className="icon-button" title="Exportar anotação" aria-label="Exportar anotação" onClick={()=>downloadBlob(new Blob([`# ${d.title}\n\n${d.content||''}`],{type:'text/markdown'}),'anotacao.md')}><Download size={16}/></button>}
 {module==='documents'?<button className="icon-button" aria-label="Baixar arquivo privado" onClick={()=>action(async()=>downloadBlob(await api(`/documents/${record.id}/download`,{download:true}),d.filename||'documento'))}><Download size={17}/></button>:<button className="icon-button" aria-label={`Editar ${d.title}`} title="Editar" onClick={edit}><Pencil size={16}/></button>}
 <button className="icon-button delete-button" aria-label={`Excluir ${d.title}`} title="Excluir" onClick={remove} disabled={busy}><Trash2 size={16}/></button>
 </div></article>;
}
