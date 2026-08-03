import Image from "next/image";

export default function Home() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-between p-24" >
      <h1 className="text-4xl font-bold">Welcome to Health Client</h1>
      <button className ="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded">
        Get Started
      </button>
    </main>
  );
}
