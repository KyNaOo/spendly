import { render, screen } from '@testing-library/angular';
import { PageHeader } from './page-header';

describe('PageHeader', () => {
  it('renders the title and the subtitle', async () => {
    await render(PageHeader, { inputs: { title: 'Titre', subtitle: 'Sous-titre' } });

    expect(screen.getByRole('heading', { level: 1, name: 'Titre' })).toBeInTheDocument();
    expect(screen.getByText('Sous-titre')).toBeInTheDocument();
  });

  it('omits the subtitle when none is given', async () => {
    const { container } = await render(PageHeader, { inputs: { title: 'Titre' } });

    expect(container.querySelector('.page-header__subtitle')).toBeNull();
  });
});
