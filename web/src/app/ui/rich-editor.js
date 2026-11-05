"use client";
import {useEditor,EditorContent} from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import {TableKit} from '@tiptap/extension-table';
import TaskList from '@tiptap/extension-task-list';
import TaskItem from '@tiptap/extension-task-item';
import {Bold,Italic,List,ListOrdered,Code,Link as LinkIcon,Table as TableIcon,CheckSquare,Heading2,Undo,Redo} from 'lucide-react';
import DOMPurify from 'dompurify';
import './editor.css';
export default function RichEditor({html,text,onChange}){
 const editor=useEditor({extensions:[StarterKit.configure({link:{openOnClick:false,protocols:['https','http']}}),TableKit,TaskList,TaskItem.configure({nested:true})],immediatelyRender:false,content:html?DOMPurify.sanitize(html):{type:'doc',content:[{type:'paragraph',content:text?[{type:'text',text}]:[]}]},onUpdate:({editor})=>onChange({html:editor.getHTML(),content:editor.getText()}),editorProps:{attributes:{'aria-label':'Conteúdo da anotação',role:'textbox','aria-multiline':'true'}}});
 if(!editor)return <p className="muted">Preparando seu caderno…</p>;
 const commands=[['Título',Heading2,()=>editor.chain().focus().toggleHeading({level:2}).run(),'heading'],['Negrito',Bold,()=>editor.chain().focus().toggleBold().run(),'bold'],['Itálico',Italic,()=>editor.chain().focus().toggleItalic().run(),'italic'],['Lista',List,()=>editor.chain().focus().toggleBulletList().run(),'bulletList'],['Lista numerada',ListOrdered,()=>editor.chain().focus().toggleOrderedList().run(),'orderedList'],['Checklist',CheckSquare,()=>editor.chain().focus().toggleTaskList().run(),'taskList'],['Código',Code,()=>editor.chain().focus().toggleCodeBlock().run(),'codeBlock'],['Tabela',TableIcon,()=>editor.chain().focus().insertTable({rows:3,cols:3,withHeaderRow:true}).run(),'table'],['Link',LinkIcon,()=>{const href=window.prompt('Endereço completo (https://…)');if(href&&/^https?:\/\//.test(href))editor.chain().focus().setLink({href}).run();},'link'],['Desfazer',Undo,()=>editor.chain().focus().undo().run()],['Refazer',Redo,()=>editor.chain().focus().redo().run()]];
 return <div className="rich-editor"><div className="editor-toolbar" role="toolbar" aria-label="Formatar anotação">{commands.map(([label,Icon,run,active])=><button key={label} type="button" title={label} aria-label={label} aria-pressed={active?editor.isActive(active):undefined} className={`icon-button ${active&&editor.isActive(active)?'active':''}`} onClick={run}><Icon size={16}/></button>)}{editor.isActive('table')&&<><button type="button" className="small-button" onClick={()=>editor.chain().focus().addRowAfter().run()}>+ linha</button><button type="button" className="small-button" onClick={()=>editor.chain().focus().addColumnAfter().run()}>+ coluna</button><button type="button" className="small-button" onClick={()=>editor.chain().focus().deleteTable().run()}>Excluir tabela</button></>}</div><EditorContent editor={editor}/></div>;
}
