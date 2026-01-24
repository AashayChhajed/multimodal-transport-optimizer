import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { PackagePlus, BarChart3, Route, Truck } from "lucide-react";
import { Progress } from "@/components/ui/progress";

export default function Dashboard() {
  return (
    <div className="space-y-8">
      <div className="flex justify-center p-10">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 w-full max-w-3xl">
          <Card className="hover:shadow-md transition">
            <CardContent className="flex flex-col items-center justify-center gap-4 p-8">
              <PackagePlus className="h-10 w-10" />
              <p className="text-lg font-semibold">Create Shipment</p>
              <Button size="lg" className="w-full">Start</Button>
            </CardContent>
          </Card>

          <Card className="hover:shadow-md transition">
            <CardContent className="flex flex-col gap-4 p-8">
              <div className="flex items-center gap-2">
                <Route className="h-8 w-8" />
                <p className="text-lg font-semibold">Track Shipment</p>
              </div>
              <input
                placeholder="Enter Shipment ID"
                className="h-12 w-full rounded-md border bg-background px-4 text-sm outline-none focus:ring-2 focus:ring-ring"
              />
              <Button size="lg" variant="secondary" className="w-full">Track</Button>
            </CardContent>
          </Card>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Total Shipments" value="1,248" icon={Truck} />
        <StatCard title="Completed" value="1,010" icon={BarChart3} />
        <StatCard title="In Progress" value="238" icon={Route} />
        <StatCard title="Pending" value="56" icon={PackagePlus} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-1">
          <CardHeader>
            <CardTitle>Shipment Completion</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Progress value={81} />
            <p className="text-sm text-muted-foreground">81% shipments completed</p>
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Shipments Over Time</CardTitle>
          </CardHeader>
          <CardContent className="h-[260px] flex items-center justify-center text-muted-foreground">
            Graph goes here (Recharts)
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
