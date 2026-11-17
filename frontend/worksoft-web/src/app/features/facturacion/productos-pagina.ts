import { Component, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, switchMap } from 'rxjs';
import { CompanyContext } from '../workspace/company-context';
import { FacturacionApi, ProductoVenta, ValoresProducto } from './facturacion.api';
import { NavegacionFacturacion } from './navegacion-facturacion';
import { apiErrorMessage } from '../../core/api-error';
import { formatoImporte, importeNormalizado } from './calculos-venta';
@Component({
  selector: 'app-productos-pagina',
  imports: [FormsModule, NavegacionFacturacion],
  templateUrl: './productos-pagina.html',
})
export class ProductosPagina {
  readonly contexto = inject(CompanyContext);
  private readonly api = inject(FacturacionApi);
  private readonly destruir = inject(DestroyRef);
  readonly productos = signal<ProductoVenta[]>([]);
  readonly total = signal(0);
  readonly pagina = signal(0);
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly editado = signal<ProductoVenta | null>(null);
  busqueda = '';
  private consulta = '';
  valores = this.inicial();
  readonly importe = formatoImporte;
  private inicial(): ValoresProducto {
    return {
      codigo: '',
      nombre: '',
      tipo: 'SERVICIO',
      unidad: 'UN',
      precio: '0.00',
      tratamientoIva: 'GRAVADO',
      tarifaIva: '',
      activo: true,
      version: 0,
    };
  }
  constructor() {
    effect(() => {
      if (this.contexto.company())
        untracked(() => {
          this.nuevo();
          this.cargar();
        });
    });
  }
  nuevo() {
    if (this.ocupado()) return;
    this.editado.set(null);
    this.valores = this.inicial();
    this.error.set('');
  }
  editar(producto: ProductoVenta) {
    if (this.ocupado()) return;
    this.editado.set(producto);
    this.valores = { ...producto };
    this.error.set('');
  }
  buscar() {
    this.consulta = this.busqueda.trim();
    this.cargar();
  }
  cargar(pagina = 0) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.api
      .productos(this.contexto.company()!.id, this.consulta, pagina)
      .pipe(
        takeUntilDestroyed(this.destruir),
        finalize(() => this.ocupado.set(false)),
      )
      .subscribe({
        next: (r) => {
          this.productos.set(r.items);
          this.total.set(r.total);
          this.pagina.set(r.page);
        },
        error: (e) => this.error.set(apiErrorMessage(e)),
      });
  }
  guardar() {
    if (this.ocupado()) return;
    this.error.set('');
    let valores: ValoresProducto;
    try {
      valores = {
        ...this.valores,
        precio: importeNormalizado(this.valores.precio),
        tarifaIva: importeNormalizado(
          this.valores.tratamientoIva === 'GRAVADO' ? this.valores.tarifaIva : '0',
        ),
      };
    } catch {
      this.error.set(
        'Revisa el precio y la tarifa. Usa hasta dos decimales, sin separadores de miles.',
      );
      return;
    }
    this.ocupado.set(true);
    this.api
      .guardarProducto(this.contexto.company()!.id, this.editado()?.id ?? null, valores)
      .pipe(
        switchMap((producto) => {
          this.editado.set(producto);
          this.valores = { ...producto };
          this.mensaje.set('Producto o servicio guardado en PostgreSQL.');
          return this.api.productos(this.contexto.company()!.id, this.consulta, this.pagina());
        }),
        takeUntilDestroyed(this.destruir),
        finalize(() => this.ocupado.set(false)),
      )
      .subscribe({
        next: (pagina) => {
          this.productos.set(pagina.items);
          this.total.set(pagina.total);
          this.pagina.set(pagina.page);
        },
        error: (e) => this.error.set(apiErrorMessage(e)),
      });
  }
}
