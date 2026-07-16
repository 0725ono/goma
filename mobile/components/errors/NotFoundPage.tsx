import { StyleSheet, Text, View, TouchableOpacity } from "react-native";
import { useRouter } from "expo-router";

export const NotFoundPage = () => {
  const router = useRouter();

  return (
    <View style={styles.container}>
      <Text style={styles.title}>404</Text>
      <Text style={styles.message}>ページが見つかりませんでした</Text>
      <TouchableOpacity
        style={styles.button}
        onPress={() => router.replace("/(private)")}
      >
        <Text style={styles.buttonText}>ホームに戻る</Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#F5F5F5",
  },
  title: { fontSize: 48, fontWeight: "bold", color: "#333" },
  message: { fontSize: 18, color: "#666", marginBottom: 20 },
  button: { backgroundColor: "#007AFF", padding: 12, borderRadius: 8 },
  buttonText: { color: "#FFF", fontWeight: "bold" },
});
