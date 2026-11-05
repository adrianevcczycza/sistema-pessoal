"use client";
import {useState} from 'react';
import {ChevronLeft,ChevronRight,Plus} from 'lucide-react';
import './planning.css';
export default function Agenda({records,today,onEdit}){
 const [mode,setMode]=useState('month'),[anchor,setAnchor]=useState(today);
 const date=new Date(`${anchor||today}T12:00:00`);const start=new Date(date);let count=1;
 if(mode==='month'){start.setDate(1);start.setDate(1-((start.getDay()+6)%7));count=42;}
 if(mode==='week'){start.setDate(start.getDate()-((start.getDay()+6)%7));count=7;}
 const iso=d=>`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
 function shift(direction){const d=new Date(date);if(mode==='month')d.setMonth(d.getMonth()+direction);else d.setDate(d.getDate()+direction*(mode==='week'?7:1));setAnchor(iso(d));}
 return <section className="panel agenda-calendar"><div className="calendar-controls"><div><button className="icon-button" aria-label="Período anterior" onClick={()=>shift(-1)}><ChevronLeft size={18}/></button><strong>{date.toLocaleDateString('pt-BR',{month:'long',year:'numeric',...(mode==='day'?{day:'numeric'}:{})})}</strong><button className="icon-button" aria-label="Próximo período" onClick={()=>shift(1)}><ChevronRight size={18}/></button></div><div><button className="small-button" onClick={()=>setAnchor(today)}>Hoje</button><select aria-label="Período da agenda" value={mode} onChange={e=>setMode(e.target.value)}><option value="day">Dia</option><option value="week">Semana</option><option value="month">Mês</option></select></div></div><div className={`calendar-grid ${mode==='day'?'one-day':''}`}>{Array.from({length:count},(_,i)=>{const d=new Date(start);d.setDate(d.getDate()+i);const day=iso(d);const tasks=records.filter(r=>(r.data.date||r.data.dueDate)===day);return <div className={`calendar-day ${day===today?'today':''}`} key={day}><div><span>{d.toLocaleDateString('pt-BR',{weekday:'short'})} <strong>{d.getDate()}</strong></span><button aria-label={`Adicionar tarefa em ${day}`} onClick={()=>onEdit(null,{date:day})}><Plus size={13}/></button></div>{tasks.map(r=><button key={r.id} className={`calendar-task ${r.data.done?'done':''}`} onClick={()=>onEdit(r)}>{r.data.done?'✓ ':''}{r.data.time&&`${r.data.time} · `}{r.data.title}</button>)}</div>;})}</div></section>;
}
