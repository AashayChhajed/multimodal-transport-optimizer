import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { PackagePlus, BarChart3, Route, Truck } from "lucide-react";
import { getShipments, getDashboardStats } from "@/lib/api";

export default async function Dashboard() {
  const shipments = await getShipments();
  const latestShipmentId = shipments.length > 0 ? shipments[shipments.length - 1].id : null;

  let stats = null;
  try {
    stats = await getDashboardStats();
  } catch {
    // Stats endpoint may not be available yet
  }

  return (
    <div className="space-y-8">
      <div className="flex justify-center p-10">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 w-full max-w-3xl">
          <Card className="hover:shadow-md transition">
            <CardContent className="flex flex-col items-center justify-center gap-4 p-8">
              <PackagePlus className="h-10 w-10" />
              <p className="text-lg font-semibold">Create Shipment</p>
              <Button size="lg" className="w-full" asChild>
                <Link href="/create-shipment">Start</Link>
              </Button>
            </CardContent>
          </Card>

          <Card className="hover:shadow-md transition">
            <CardContent className="flex flex-col gap-4 p-8">
              <div className="flex items-center gap-2">
                <Route className="h-8 w-8" />
                <p className="text-lg font-semibold">Track Shipment</p>
              </div>
              <p className="text-sm text-muted-foreground">
                {latestShipmentId
                  ? `Latest shipment ID available: ${latestShipmentId}`
                  : "No shipments yet. Create one to start tracking."}
              </p>
              <Button size="lg" variant="secondary" className="w-full" asChild>
                <Link href={latestShipmentId ? `/route/${latestShipmentId}` : "/create-shipment"}>
                  Track Latest
                </Link>
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Total Shipments" value={String(stats?.totalShipments ?? shipments.length)} icon={Truck} />
        <StatCard title="Optimized" value={String(stats?.optimizedShipments ?? 0)} icon={BarChart3} />
        <StatCard title="Total Routes" value={String(stats?.totalRoutes ?? 0)} icon={Route} />
        <StatCard title="Avg Cost" value={stats ? `$${stats.averageCost.toFixed(2)}` : "—"} icon={PackagePlus} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-1">
          <CardHeader>
            <CardTitle>Average Delivery Time</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <p className="text-2xl font-bold">
              {stats ? `${stats.averageTime.toFixed(1)} h` : "—"}
            </p>
            <p className="text-sm text-muted-foreground">
              Across all optimized shipments
            </p>
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Total Distance &amp; Carbon</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-4">
              <div className="rounded-lg border p-4">
                <p className="text-xs text-muted-foreground">Total Distance</p>
                <p className="text-lg font-semibold">
                  {stats ? `${stats.totalDistance.toFixed(0)} km` : "—"}
                </p>
              </div>
              <div className="rounded-lg border p-4">
                <p className="text-xs text-muted-foreground">Total Carbon</p>
                <p className="text-lg font-semibold">
                  {stats ? `${stats.totalCarbon.toFixed(1)} kg CO₂` : "—"}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function StatCard({ title, value, icon: Icon }: { title: string; value: string; icon: React.ComponentType<{ className: string }> }) {
  return (
    <Card>
      <CardContent className="flex items-center justify-between p-6">
        <div>
          <p className="text-sm text-muted-foreground">{title}</p>
          <p className="text-2xl font-bold">{value}</p>
        </div>
        <Icon className="h-8 w-8 text-muted-foreground" />
      </CardContent>
    </Card>
  );
}
