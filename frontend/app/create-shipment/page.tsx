import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";

export default function CreateShipment() {
  return (
    <div className="min-h-screen flex items-center justify-center p-6">
      <Card className="w-full max-w-3xl">
        <CardHeader>
          <CardTitle>Create Shipment</CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Weight (kg)</Label>
              <Input type="number" placeholder="e.g. 120" />
            </div>
            <div className="space-y-2">
              <Label>Dimensions (L x W x H)</Label>
              <Input placeholder="e.g. 40 x 30 x 20" />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Shipment Type</Label>
              <Select>
                <SelectTrigger>
                  <SelectValue placeholder="Select type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="fragile">Fragile</SelectItem>
                  <SelectItem value="liquid">Liquid</SelectItem>
                  <SelectItem value="perishable">Perishable</SelectItem>
                  <SelectItem value="general">General</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Priority</Label>
              <RadioGroup defaultValue="low" className="flex gap-6">
                <div className="flex items-center gap-2">
                  <RadioGroupItem value="high" />
                  <Label>High</Label>
                </div>
                <div className="flex items-center gap-2">
                  <RadioGroupItem value="low" />
                  <Label>Low</Label>
                </div>
              </RadioGroup>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Source</Label>
              <Input placeholder="City / Warehouse" />
            </div>
            <div className="space-y-2">
              <Label>Destination</Label>
              <Input placeholder="City / Warehouse" />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Owner Name</Label>
              <Input placeholder="Full name" />
            </div>
            <div className="space-y-2">
              <Label>Mobile Number</Label>
              <Input type="tel" placeholder="e.g. 9876543210" />
            </div>
          </div>

          <div className="space-y-2">
            <Label>Owner Address</Label>
            <Textarea placeholder="Full address" />
          </div>

          <div className="space-y-2">
            <Label>Authorization Person ID</Label>
            <Input placeholder="Employee / Govt ID" />
          </div>

          <div className="flex justify-end">
            <Button size="lg">Create Shipment</Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}