export interface GeoLocationResult {
  latitude: number;
  longitude: number;
  accuracy: number;
  address: string;
  googleMapsUrl: string;
}

export async function getCurrentGeoLocation(): Promise<GeoLocationResult> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('Geolocation is not supported by your browser or device.'));
      return;
    }

    navigator.geolocation.getCurrentPosition(
      async position => {
        const { latitude, longitude, accuracy } = position.coords;
        const googleMapsUrl = `https://www.google.com/maps?q=${latitude},${longitude}`;

        // Attempt reverse geocoding via OpenStreetMap Nominatim
        let resolvedAddress = '';
        try {
          const controller = new AbortController();
          const timeoutId = setTimeout(() => controller.abort(), 4000);

          const response = await fetch(
            `https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}&zoom=18&addressdetails=1`,
            {
              headers: { 'Accept-Language': 'en' },
              signal: controller.signal
            }
          );
          clearTimeout(timeoutId);

          if (response.ok) {
            const data = await response.json();
            if (data && data.display_name) {
              // Extract meaningful components
              const addr = data.address || {};
              const parts = [
                addr.road || addr.pedestrian || addr.suburb,
                addr.neighbourhood || addr.city_district || addr.quarter,
                addr.city || addr.town || addr.county || 'Dhaka',
                addr.postcode
              ].filter(Boolean);

              resolvedAddress = parts.length > 0 ? parts.join(', ') : data.display_name;
            }
          }
        } catch {
          // Graceful fallback if offline or request blocked
        }

        if (!resolvedAddress) {
          resolvedAddress = `Location (${latitude.toFixed(5)}, ${longitude.toFixed(5)})`;
        }

        resolve({
          latitude,
          longitude,
          accuracy,
          address: resolvedAddress,
          googleMapsUrl
        });
      },
      error => {
        let msg = 'Failed to retrieve location.';
        switch (error.code) {
          case error.PERMISSION_DENIED:
            msg = 'Location permission was denied. Please allow location access in your browser or phone settings.';
            break;
          case error.POSITION_UNAVAILABLE:
            msg = 'GPS or Network position is unavailable.';
            break;
          case error.TIMEOUT:
            msg = 'Location request timed out. Please try again.';
            break;
        }
        reject(new Error(msg));
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 30000
      }
    );
  });
}
