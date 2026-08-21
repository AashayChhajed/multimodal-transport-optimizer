"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { createShipment, getCities, optimizeShipment, type City, type OptimizationType } from "@/lib/api";

export default function CreateShipment() {
  const router = useRouter();
  const [cities, setCities] = useState<City[]>([]);
  const [sourceCityId, setSourceCityId] = useState<string>("");
  const [destinationCityId, setDestinationCityId] = useState<string>("");
  const [weight, setWeight] = useState<string>("1200");
  const [description, setDescription] = useState<string>("Electronics");
  const [optimizationType, setOptimizationType] = useState<OptimizationType>("CHEAPEST");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string>("");

  useEffect(() => {
    const loadCities = async () => {
      try {
        const cityData = await getCities();
        setCities(cityData);
        if (cityData.length >= 2) {
          setSourceCityId(String(cityData[0].id));
          setDestinationCityId(String(cityData[1].id));
        }
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "Failed to load cities");
      }
    };

    loadCities();
  }, []);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage("");

    if (!sourceCityId || !destinationCityId) {
      setErrorMessage("Please select source and destination cities.");
      return;
    }

    if (sourceCityId === destinationCityId) {
      setErrorMessage("Source and destination cannot be the same city.");
      return;
    }

    setIsSubmitting(true);
    try {
      const created = await createShipment({
        sourceCityId: Number(sourceCityId),
        destinationCityId: Number(destinationCityId),
        weight: Number(weight),
        description,
      });

      await optimizeShipment(created.id, optimizationType);
      router.push(`/optimization/${created.id}`);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Failed to create shipment");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="p-6">
      <Card className="mx-auto w-full max-w-3xl">
        <CardHeader>
          <CardTitle>Create Shipment</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="space-y-6" onSubmit={handleSubmit}>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="sourceCity">Source City</Label>
                <Select value={sourceCityId} onValueChange={setSourceCityId}>
                  <SelectTrigger id="sourceCity" className="w-full">
                    <SelectValue placeholder="Select source city" />
                  </SelectTrigger>
                  <SelectContent>
                    {cities.map((city) => (
                      <SelectItem key={city.id} value={String(city.id)}>
                        {city.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="destinationCity">Destination City</Label>
                <Select value={destinationCityId} onValueChange={setDestinationCityId}>
                  <SelectTrigger id="destinationCity" className="w-full">
                    <SelectValue placeholder="Select destination city" />
                  </SelectTrigger>
                  <SelectContent>
                    {cities.map((city) => (
                      <SelectItem key={city.id} value={String(city.id)}>
                        {city.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="weight">Weight (kg)</Label>
                <Input
                  id="weight"
                  type="number"
                  min="1"
                  step="0.1"
                  value={weight}
                  onChange={(event) => setWeight(event.target.value)}
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="optimizationType">Optimization Goal</Label>
                <Select
                  value={optimizationType}
                  onValueChange={(value) => setOptimizationType(value as OptimizationType)}
                >
                  <SelectTrigger id="optimizationType" className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="CHEAPEST">Cheapest</SelectItem>
                    <SelectItem value="FASTEST">Fastest</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Shipment Description</Label>
              <Textarea
                id="description"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                placeholder="Describe the shipment"
                required
              />
            </div>

            {errorMessage && (
              <p className="text-sm text-red-600">{errorMessage}</p>
            )}

            <div className="flex justify-end">
              <Button type="submit" size="lg" disabled={isSubmitting}>
                {isSubmitting ? "Creating..." : "Create & Optimize"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}