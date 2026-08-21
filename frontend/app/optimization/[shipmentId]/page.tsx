import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { getOptimizationResult } from "@/lib/api";

export default async function OptimizationResultsPage({
  params,
}: {
  params: Promise<{ shipmentId: string }>;
}) {
  const { shipmentId } = await params;
  const result = await getOptimizationResult(Number(shipmentId));

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Optimization Results</h1>
        <p className="text-sm text-muted-foreground">Shipment ID: {shipmentId}</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-3">
            Selected Path
            {result.optimizationType && <Badge variant="outline">{result.optimizationType}</Badge>}
          </CardTitle>
          <CardDescription>Computed by A* over multimodal transport routes</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-lg border p-4">
              <p className="text-xs text-muted-foreground">Total Cost</p>
              <p className="text-lg font-semibold">${result.totalCost.toFixed(2)}</p>
            </div>
            <div className="rounded-lg border p-4">
              <p className="text-xs text-muted-foreground">Total Time</p>
              <p className="text-lg font-semibold">{result.totalTime.toFixed(2)} hours</p>
            </div>
            <div className="rounded-lg border p-4">
              <p className="text-xs text-muted-foreground">Total Distance</p>
              <p className="text-lg font-semibold">{result.totalDistance.toFixed(0)} km</p>
            </div>
            <div className="rounded-lg border p-4">
              <p className="text-xs text-muted-foreground">Carbon Emissions</p>
              <p className="text-lg font-semibold">{result.totalCarbon.toFixed(1)} kg CO₂</p>
            </div>
          </div>

          <div>
            <p className="mb-2 text-sm font-medium">City Path</p>
            <p className="text-sm text-muted-foreground">{result.cities.join(" -> ")}</p>
          </div>

          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Leg</TableHead>
                <TableHead>From</TableHead>
                <TableHead>To</TableHead>
                <TableHead>Mode</TableHead>
                <TableHead>Distance (km)</TableHead>
                <TableHead>Cost</TableHead>
                <TableHead>Time (h)</TableHead>
                <TableHead>Carbon (kg)</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {result.routes.map((step, index) => (
                <TableRow key={`${step.sourceCity}-${step.destinationCity}-${index}`}>
                  <TableCell>{index + 1}</TableCell>
                  <TableCell>{step.sourceCity}</TableCell>
                  <TableCell>{step.destinationCity}</TableCell>
                  <TableCell>{step.transportMode}</TableCell>
                  <TableCell>{step.distance.toFixed(2)}</TableCell>
                  <TableCell>${step.cost.toFixed(2)}</TableCell>
                  <TableCell>{step.time.toFixed(2)}</TableCell>
                  <TableCell>{step.carbon.toFixed(2)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </section>
  );
}
