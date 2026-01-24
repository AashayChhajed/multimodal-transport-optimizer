import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"

export default function Home() {
  return (
    <div className="p-6 max-w-md mx-auto">
      <Card>
        <CardHeader>
          <CardTitle>Shadcn Card</CardTitle>
          <CardDescription>This is a simple card component</CardDescription>
        </CardHeader>

        <CardContent>
          <p className="text-sm text-muted-foreground">
            This is the card content area where you put main information.
          </p>
        </CardContent>

        <CardFooter>
          <p className="text-xs text-muted-foreground">
            Footer content here
          </p>
        </CardFooter>
      </Card>
    </div>
  )
}
