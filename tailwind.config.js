/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        ebl: {
          navy: {
            dark: '#091E3A',
            primary: '#0F325E',
            secondary: '#1E4B82',
            light: '#E8EEF5',
            container: '#D6E2F0'
          },
          gold: {
            DEFAULT: '#C4973B',
            light: '#FBF4E4',
            dark: '#967022'
          }
        }
      }
    },
  },
  plugins: [],
}
