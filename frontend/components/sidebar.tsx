'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { LayoutDashboard, Plus, BarChart3, Map, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useSidebar } from './sidebar-context';

interface NavItem {
  label: string;
  href: string;
  icon: React.ReactNode;
}

const navItems: NavItem[] = [
  {
    label: 'Dashboard',
    href: '/',
    icon: <LayoutDashboard className="w-5 h-5" />,
  },
  {
    label: 'Create Shipment',
    href: '/create-shipment',
    icon: <Plus className="w-5 h-5" />,
  },
  {
    label: 'Optimization Result',
    href: '/optimization-result',
    icon: <BarChart3 className="w-5 h-5" />,
  },
  {
    label: 'Routes',
    href: '/routes',
    icon: <Map className="w-5 h-5" />,
  },
];

export function Sidebar() {
  const pathname = usePathname();
  const { isOpen, toggle } = useSidebar();

  return (
    <aside
      className={cn(
        'w-64 bg-slate-900 text-white min-h-screen p-6 fixed left-0 top-0 transition-transform duration-300 z-40',
        isOpen ? 'translate-x-0' : '-translate-x-full'
      )}
    >
      {/* Header with Close Button */}
      <div className="flex items-center justify-between mb-12">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <div className="w-10 h-10 bg-blue-600 rounded-lg flex items-center justify-center">
              <Map className="w-6 h-6" />
            </div>
            MTO
          </h1>
          <p className="text-slate-400 text-sm mt-1">Multimodal Transport</p>
        </div>
        <button
          onClick={toggle}
          className="p-1 hover:bg-slate-800 rounded-lg transition-colors"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Navigation */}
      <nav className="space-y-2">
        {navItems.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={cn(
              'flex items-center gap-3 px-4 py-3 rounded-lg transition-colors',
              pathname === item.href
                ? 'bg-blue-600 text-white'
                : 'text-slate-300 hover:bg-slate-800 hover:text-white'
            )}
          >
            {item.icon}
            <span className="font-medium">{item.label}</span>
          </Link>
        ))}
      </nav>

      {/* Footer */}
      <div className="absolute bottom-6 left-6 right-6">
        <div className="bg-slate-800 rounded-lg p-4">
          <p className="text-xs text-slate-400">Logged in as</p>
          <p className="text-sm font-semibold text-white mt-1">Admin User</p>
        </div>
      </div>
    </aside>
  );
}
