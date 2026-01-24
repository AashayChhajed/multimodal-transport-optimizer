'use client';

import { use, useState } from "react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  LayoutDashboard,
  PackagePlus,
  Route,
  BarChart3,
  Menu
} from "lucide-react";

const navItems = [
  { label: "Dashboard", icon: LayoutDashboard, key: "dashboard" },
  { label: "Create Shipment", icon: PackagePlus, key: "create" },
  { label: "Optimization Result", icon: BarChart3, key: "optimization" },
  { label: "Routes", icon: Route, key: "routes" },
];

export default function Sidebar() {
  const [collapsed, setCollapsed] = useState(false);
  const [active, setActive] = useState("dashboard");

  return (
    <aside
      className={cn(
        "h-screen border-r bg-background transition-all duration-300 flex flex-col",
        collapsed ? "w-16" : "w-64"
      )}
    >
      {/* Top toggle */}
      <div className="flex items-center justify-between p-3 border-b">
        {!collapsed && <span className="font-semibold">Transport Optimizer</span>}
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setCollapsed(!collapsed)}
        >
          {collapsed ? <Menu /> : <Menu/>}
        </Button>
      </div>

      {/* Navigation */}
      <nav className="flex-1 p-2 space-y-1">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = active === item.key;

          return (
            <Button
              key={item.key}
              variant={isActive ? "secondary" : "ghost"}
              className={cn(
                "w-full justify-start gap-3",
                collapsed && "justify-center"
              )}
              onClick={() => setActive(item.key)}
            >
              <Icon className="h-5 w-5" />
              {!collapsed && <span>{item.label}</span>}
            </Button>
          );
        })}
      </nav>
    </aside>
  );
}
