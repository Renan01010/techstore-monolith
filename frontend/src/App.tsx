import { ApiStatus } from './components/ApiStatus';

export default function App() {
  return (
    <main className="foundation">
      <header className="brand">
        <a className="brand__link" href="/" aria-label="TechStore, início">
          <span className="brand__symbol" aria-hidden="true">T</span>
          <span>techstore</span>
        </a>
        <span className="brand__caption">TECNOLOGIA PARA O SEU DIA</span>
      </header>

      <section className="welcome" aria-labelledby="welcome-title">
        <p className="eyebrow">NOVA EXPERIÊNCIA, MESMA ESSÊNCIA</p>
        <h1 id="welcome-title">Estamos preparando a próxima geração da TechStore.</h1>
        <p className="welcome__description">
          Uma base mais simples e integrada para você encontrar a tecnologia certa.
        </p>
        <ApiStatus />
      </section>

      <footer className="foundation__footer">
        <span>TechStore</span>
        <span>Uma nova base. O mesmo compromisso.</span>
      </footer>
    </main>
  );
}
