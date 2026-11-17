import { Component, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CompanyContext } from '../workspace/company-context';
import { FacturacionApi, CarteraVenta, ResumenVenta } from './facturacion.api';
import { NavegacionFacturacion } from './navegacion-facturacion';
import { apiErrorMessage } from '../../core/api-error';
import { formatoImporte, fechaBogota } from './calculos-venta';
@Component({
  selector: 'app-cartera-pagina',
  imports: [FormsModule, RouterLink, NavegacionFacturacion],
  templateUrl: './cartera-pagina.html',
})
export class CarteraPagina {
  readonly contexto = inject(CompanyContext);
  private readonly api = inject(FacturacionApi);
  private readonly destruir = inject(DestroyRef);
  readonly resumen = signal<CarteraVenta | null>(null);
  readonly ventas = signal<ResumenVenta[]>([]);
  readonly total = signal(0);
  readonly pagina = signal(0);
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly importe = formatoImporte;
  corte = fechaBogota();
  busqueda = '';
  constructor() {
    effect(() => {
      if (this.contexto.company()) untracked(() => void this.cargar());
    });
  }
  async cargar(pagina = 0) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.error.set('');
    try {
      const empresa = this.contexto.company()!.id;
      const [resumen, ventas] = await Promise.all([
        firstValueFrom(
          this.api.cartera(empresa, this.corte).pipe(takeUntilDestroyed(this.destruir)),
        ),
        firstValueFrom(
          this.api
            .documentos(empresa, {
              q: this.busqueda,
              estado: 'CONFIRMADO',
              tipo: 'VENTAS',
              page: pagina,
              size: 25,
            })
            .pipe(takeUntilDestroyed(this.destruir)),
        ),
      ]);
      this.resumen.set(resumen);
      this.ventas.set(ventas.items);
      this.total.set(ventas.total);
      this.pagina.set(pagina);
    } catch (e) {
      this.error.set(apiErrorMessage(e));
    } finally {
      this.ocupado.set(false);
    }
  }
}
