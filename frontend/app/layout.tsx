import "./globals.css"
import Sidebar from "@/components/layout/sidebar"
import { ThemeProvider } from "next-themes"
import ThemeToggle from "@/components/layout/theme-toggle"

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body>
        <ThemeProvider attribute="class" defaultTheme="system" enableSystem>
          <div className="flex min-h-screen">
            <Sidebar />
            <main className="relative flex-1 p-6">
              <div className="absolute top-6 right-6 z-50">
                <ThemeToggle />
              </div>
              {children}
            </main>
          </div>
        </ThemeProvider>
      </body>
    </html>
  )
}