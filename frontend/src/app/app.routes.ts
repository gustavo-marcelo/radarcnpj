import { Routes } from '@angular/router';

import { adminGuard } from './core/auth/admin.guard';
import { authGuard } from './core/auth/auth.guard';
import { CadastroUsuario } from './pages/cadastro/cadastro-usuario';
import { Login } from './pages/login/login';
import { Layout } from './pages/layout/layout';
import { EmpresasLista } from './pages/empresas/empresas-lista';
import { EmpresaDetalhe } from './pages/empresas/empresa-detalhe';
import { EstabelecimentosLista } from './pages/estabelecimentos/estabelecimentos-lista';
import { EstabelecimentoDetalhe } from './pages/estabelecimentos/estabelecimento-detalhe';
import { Cnaes } from './pages/cnaes/cnaes';
import { Usuarios } from './pages/usuarios/usuarios';
import { ImportacaoAdmin } from './pages/importacao/importacao';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'cadastro', component: CadastroUsuario },
  {
    path: '',
    component: Layout,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'empresas', pathMatch: 'full' },
      { path: 'empresas', component: EmpresasLista },
      { path: 'empresas/:cnpjBasico', component: EmpresaDetalhe },
      { path: 'estabelecimentos', component: EstabelecimentosLista },
      { path: 'estabelecimentos/:cnpjBasico/:cnpjOrdem/:cnpjDv', component: EstabelecimentoDetalhe },
      { path: 'cnaes', component: Cnaes },
      { path: 'importacao', component: ImportacaoAdmin, canActivate: [adminGuard] },
      { path: 'usuarios', component: Usuarios, canActivate: [adminGuard] },
      { path: '**', redirectTo: 'empresas' },
    ],
  },
];