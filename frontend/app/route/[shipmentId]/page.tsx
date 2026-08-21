import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";
import { getOptimizationResult } from "@/lib/api";

export default async function RouteDetailsPage({
  params,
}: {
  params: Promise<{ shipmentId: string }>;
}) {
  const { shipmentId } = await params;
  const result = await getOptimizationResult(Number(shipmentId));

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Route Details</h1>
        <p className="text-sm text-muted-foreground">Shipment ID: {shipmentId}</p>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Route Summary</CardTitle>
            <CardDescription>Computed city sequence for selected shipment</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm text-muted-foreground">{result.cities.join(" -> ")}</p>
            <div className="grid grid-cols-2 gap-4">
              <div className="rounded-lg border p-3">
                <p className="text-xs text-muted-foreground">Total Cost</p>
                <p className="text-lg font-semibold">${result.totalCost.toFixed(2)}</p>
              </div>
              <div className="rounded-lg border p-3">
                <p className="text-xs text-muted-foreground">Total Time</p>
                <p className="text-lg font-semibold">{result.totalTime.toFixed(2)} h</p>
              </div>
              <div className="rounded-lg border p-3">
                <p className="text-xs text-muted-foreground">Total Distance</p>
                <p className="text-lg font-semibold">{result.totalDistance.toFixed(0)} km</p>
              </div>
              <div className="rounded-lg border p-3">
                <p className="text-xs text-muted-foreground">Carbon Emissions</p>
                <p className="text-lg font-semibold">{result.totalCarbon.toFixed(1)} kg CO₂</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Route Legs</CardTitle>
            <CardDescription>Transport mode, distance, cost, time, and carbon for each leg</CardDescription>
          </CardHeader>
          <CardContent className="max-h-[420px] overflow-y-auto">
            <Accordion type="single" collapsible className="w-full">
              {result.routes.map((step, index) => {
                const stepKey = `${step.sourceCity}-${step.destinationCity}-${index}`;
                return (
                  <AccordionItem key={stepKey} value={stepKey}>
                    <AccordionTrigger>
                      <div className="flex w-full items-center gap-3 pr-2">
                        <span className="text-xs text-muted-foreground">Step {index + 1}</span>
                        <Badge variant="outline">{step.transportMode}</Badge>
                        <span className="text-sm">{step.sourceCity} to {step.destinationCity}</span>
                      </div>
                    </AccordionTrigger>
                    <AccordionContent>
                      <div className="grid gap-2 text-sm sm:grid-cols-4">
                        <p><span className="text-muted-foreground">Distance:</span> {step.distance.toFixed(2)} km</p>
                        <p><span className="text-muted-foreground">Cost:</span> ${step.cost.toFixed(2)}</p>
                        <p><span className="text-muted-foreground">Time:</span> {step.time.toFixed(2)} h</p>
                        <p><span className="text-muted-foreground">Carbon:</span> {step.carbon.toFixed(2)} kg CO₂</p>
                      </div>
                    </AccordionContent>
                  </AccordionItem>
                );
              })}
            </Accordion>
          </CardContent>
        </Card>
      </div>
    </section>
  );
}
