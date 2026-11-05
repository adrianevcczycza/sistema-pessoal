import "./globals.css";

export const metadata = {
  title: "Meu Espaço | Organização pessoal",
  description:
    "Seu espaço pessoal para organizar a rotina, os estudos e os planos.",
};

export default function RootLayout({ children }) {
  return (
    <html lang="pt-BR">
      <body>{children}</body>
    </html>
  );
}
